package vRouter;

import java.util.Date;

public class Block {
    private String blockHash; // 区块哈希
    private String previousHash; // 前一个区块的哈希
    private Object data; // 区块数据
    private long timestamp; // 时间戳
    public Block(String previousHash, Object data, long timestamp) {
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
        this.blockHash = calculateHash();
    }
    // 构造方法
    public Block(String previousHash, Object data) {
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = new Date().getTime();
        this.blockHash = calculateHash();
        // System.out.println("创建新区块"+this.getBlockHash());
    }

    // 计算区块哈希
    public String calculateHash() {
        String input = previousHash + data.toString() + timestamp;
        return SHA256(input); // 使用 SHA-256 计算哈希
    }

    // 模拟 SHA-256 哈希计算
    private String SHA256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    // Getter 方法
    public String getBlockHash() {
        return blockHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public Object getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}