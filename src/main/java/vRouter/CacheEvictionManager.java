package vRouter;

import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CacheEvictionManager {
    private Jedis jedis;
    private WeightCalculator weightCalculator;
    private int maxCacheSize;
    private String nodePrefix; // 节点唯一前缀
    // 获取当前节点缓存中的所有数据ID（不带前缀的原始键）
    public Set<String> getCachedDataKeys() {
        List<String> prefixedKeysList = jedis.zrange(prefixKey("cachePriority"), 0, -1);
        Set<String> prefixedKeys = new HashSet<>(prefixedKeysList);
        Set<String> rawKeys = new HashSet<>();
        for (String prefixedKey : prefixedKeys) {
            // 移除前缀，得到原始键（如 "node:123:key" -> "key"）
            rawKeys.add(prefixedKey.substring(nodePrefix.length()));
        }
        return rawKeys;
    }

    // 更新指定数据ID的activityScore并重新计算优先级
    public void updateCacheEntry(String key, double newActivityScore) {
        String prefixedKey = prefixKey(key);
        if (!jedis.exists(prefixedKey)) {
            return; // 缓存不存在该键，直接返回
        }

        // 获取现有参数
        double frequency = Double.parseDouble(jedis.hget(prefixedKey, "frequency"));
        double distance = Double.parseDouble(jedis.hget(prefixedKey, "distance"));

        // 重新计算优先级
        double newPriority = weightCalculator.calculatePriority(frequency, distance, newActivityScore);

        // 更新Redis
        jedis.hset(prefixedKey, "activityScore", String.valueOf(newActivityScore));
        jedis.hset(prefixedKey, "priority", String.valueOf(newPriority));
        jedis.zadd(prefixKey("cachePriority"), newPriority, prefixedKey); // 更新有序集合
    }
    // 新增nodeId参数，生成节点前缀
    public CacheEvictionManager(String redisHost, int redisPort, String nodeId, int maxCacheSize, double w1, double w2, double w3) {
        this.jedis = new Jedis(redisHost, redisPort);
        this.nodePrefix =   nodeId + ":"; // 前缀格式：node:123:key
        this.maxCacheSize = maxCacheSize; // 每个节点最大缓存数量（例如10）
        this.weightCalculator = new WeightCalculator(w1, w2, w3);
    }

    // 所有操作添加节点前缀
    private String prefixKey(String key) {
        return nodePrefix + key;
    }

    public double getFrequency(String key) {
        String frequencyValue = jedis.hget(prefixKey(key), "frequency");
        // 处理两种情况：值不存在或值为null
        if (frequencyValue == null) {
            return -1.0; // 返回-1表示无记录
        }
        try {
            return Double.parseDouble(frequencyValue);
        } catch (NumberFormatException e) {
            return -1.0; // 数据格式错误时返回默认
        }
    }
    public String get(String key) {
        return jedis.hget(prefixKey(key), "data");
    }

    public void addToCache(String key, double frequency, double distance, double activityScore, int data) {
        String prefixedKey = prefixKey(key);
        double priority = weightCalculator.calculatePriority(frequency, distance, activityScore);

        // 检查当前节点的缓存数量
        if (jedis.zcard(prefixKey("cachePriority")) < maxCacheSize) {
            storeInCache(prefixedKey, priority, frequency, distance, activityScore, data);
        } else {
            List<String> leastPriorityKeysList = jedis.zrange(prefixKey("cachePriority"), 0, 0);
            Set<String> leastPriorityKeys = new HashSet<>(leastPriorityKeysList);
            if (!leastPriorityKeys.isEmpty()) {
                String leastPriorityKey = leastPriorityKeys.iterator().next();
                double leastPriority = Double.parseDouble(jedis.hget(leastPriorityKey, "priority"));
                if (priority > leastPriority) {
                    evictCacheEntry(leastPriorityKey);
                    storeInCache(prefixedKey, priority, frequency, distance, activityScore, data);
                }
            }
        }
    }

    private void storeInCache(String prefixedKey, double priority, double frequency,
                              double distance, double activityScore, int data) {
        jedis.zadd(prefixKey("cachePriority"), priority, prefixedKey);
        jedis.hset(prefixedKey, "priority", String.valueOf(priority));
        jedis.hset(prefixedKey, "frequency", String.valueOf(frequency));
        jedis.hset(prefixedKey, "distance", String.valueOf(distance));
        jedis.hset(prefixedKey, "activityScore", String.valueOf(activityScore));
        jedis.hset(prefixedKey, "data", String.valueOf(data));
    }
    public void evictByDataId(String dataId) {
        String prefixedKey = prefixKey(dataId);  // 自动添加节点前缀
        jedis.del(prefixedKey);
        jedis.zrem(prefixKey("cachePriority"), prefixedKey);
    }
 public void evictCacheEntry(String prefixedKey) {
        jedis.del(prefixedKey);
        jedis.zrem(prefixKey("cachePriority"), prefixedKey);
    }

    public void close() {
        jedis.close();
    }
}