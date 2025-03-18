package vRouter;


import redis.clients.jedis.Jedis;

import java.math.BigInteger;

import redis.clients.jedis.Jedis;

public class TTLCacheManager {
    private Jedis jedis;
    private String nodePrefix; // 节点唯一前缀

    public TTLCacheManager(String redisHost, int redisPort, String nodeId) {
        this.jedis = new Jedis(redisHost, redisPort);
        this.nodePrefix = "node:" + nodeId + ":"; // 前缀格式：node:123:key
    }

    // 所有操作添加节点前缀
    private String prefixKey(String key) {
        return nodePrefix + key;
    }

    public void setTTL(String key, int ttlSeconds) {
        jedis.expire(prefixKey(key), ttlSeconds);
    }

    public long getTTL(String key) {
        return jedis.ttl(prefixKey(key));
    }

    public void delete(String key) {
        jedis.del(prefixKey(key));
    }

    public void close() {
        jedis.close();
    }
}


