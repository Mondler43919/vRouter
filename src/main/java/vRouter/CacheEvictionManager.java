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

    // 获取当前节点缓存中的所有数据ID（原始键）
    public Set<String> getCachedDataKeys() {
        // 用 HashSet 包裹返回值，避免强制类型转换错误
        Set<String> prefixedKeys = new HashSet<>(jedis.zrange(prefixKey("cachePriority"), 0, -1));
        Set<String> rawKeys = new HashSet<>();
        for (String prefixedKey : prefixedKeys) {
            rawKeys.add(prefixedKey.substring(nodePrefix.length()));
        }
        return rawKeys;
    }


    // 更新指定数据ID的activityScore并重新计算优先级
    public void updateCacheEntry(String key, double newActivityScore) {
        String prefixedKey = prefixKey(key);
        if (!jedis.exists(prefixedKey)) {
            return;
        }

        double frequency = Double.parseDouble(jedis.hget(prefixedKey, "frequency"));
        double distance = Double.parseDouble(jedis.hget(prefixedKey, "distance"));
        double newPriority = weightCalculator.calculatePriority(frequency, distance, newActivityScore);

        jedis.hset(prefixedKey, "activityScore", String.valueOf(newActivityScore));
        jedis.hset(prefixedKey, "priority", String.valueOf(newPriority));
        jedis.zadd(prefixKey("cachePriority"), newPriority, prefixedKey);
    }

    public CacheEvictionManager(String redisHost, int redisPort, String nodeId,
                                int maxCacheSize, double w1, double w2, double w3) {
        this.jedis = new Jedis(redisHost, redisPort);
        this.nodePrefix = nodeId + ":";
        this.maxCacheSize = maxCacheSize;
        this.weightCalculator = new WeightCalculator(w1, w2, w3);
    }

    private String prefixKey(String key) {
        return nodePrefix + key;
    }

    public double getFrequency(String key) {
        String frequencyValue = jedis.hget(prefixKey(key), "frequency");
        if (frequencyValue == null) return -1.0;
        try {
            return Double.parseDouble(frequencyValue);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    public String get(String key) {
        return jedis.hget(prefixKey(key), "data");
    }

    public void addToCache(String key, double frequency, double distance,
                           double activityScore, int data) {
        String prefixedKey = prefixKey(key);
        double priority = weightCalculator.calculatePriority(frequency, distance, activityScore);

        if (jedis.zcard(prefixKey("cachePriority")) < maxCacheSize) {
            storeInCache(prefixedKey, priority, frequency, distance, activityScore, data);
        } else {
            // 用 List 接收 zrange 返回值，然后包装成 Set（如需）
            List<String> leastPriorityList = jedis.zrange(prefixKey("cachePriority"), 0, 0);
            if (!leastPriorityList.isEmpty()) {
                String leastPriorityKey = leastPriorityList.get(0);
                String storedPriority = jedis.hget(leastPriorityKey, "priority");

                if (storedPriority != null && !storedPriority.isEmpty()) {
                    double leastPriority = Double.parseDouble(storedPriority);
                    if (priority > leastPriority) {
                        evictCacheEntry(leastPriorityKey);
                        storeInCache(prefixedKey, priority, frequency, distance, activityScore, data);
                    }
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
        String prefixedKey = prefixKey(dataId);
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

    public double getDistance(String key) {
        String frequencyValue = jedis.hget(prefixKey(key), "distance");
        if (frequencyValue == null) return -1.0;
        try {
            return Double.parseDouble(frequencyValue);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
}

