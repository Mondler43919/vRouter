package vRouter;


import redis.clients.jedis.Jedis;

public class ActiveUpdate {
    private Jedis jedis;

    public ActiveUpdate(Jedis jedis) {
        this.jedis = jedis;
    }

    /**
     * 删除本地存储和缓存中的数据
     * @param prefix 缓存前缀
     */
    public void deleteData(String prefix) {
        // 从缓存中删除数据
        jedis.del(prefix);
        // 从本地存储中删除数据
        // 假设我们有一个本地存储方法
        deleteFromLocalStorage(prefix);
    }

    /**
     * 从本地存储删除数据的方法（此处模拟）
     * @param prefix 缓存前缀
     */
    private void deleteFromLocalStorage(String prefix) {
        // 模拟删除本地存储中的数据
        System.out.println("删除本地存储数据: " + prefix);
    }

    /**
     * 更新缓存中的数据
     * @param prefix 缓存前缀
     * @param data 新数据
     */
    public void updateData(String prefix, String data) {
        // 更新缓存数据
        jedis.hset(prefix, "value", data);
    }
}
