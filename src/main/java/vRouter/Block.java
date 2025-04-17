package vRouter;

import java.util.Date;

public class Block {
    private String blockHash; // 区块哈希
    private String previousHash; // 前一个区块的哈希
    private BlockData data; // 区块数据
    private long timestamp; // 时间戳
    public Block(String previousHash, BlockData data, long timestamp) {
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
        this.blockHash = calculateHash();
    }
    // 构造方法
    public Block(String previousHash, BlockData data) {
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = new Date().getTime();
        this.blockHash = calculateHash();
        // System.out.println("创建新区块"+this.getBlockHash());
    }

    // 计算区块哈希
    public String calculateHash() {
        String input = previousHash + data.toString() + timestamp;
        return HashUtils.SHA256(input); // 使用 SHA-256 计算哈希
    }


    // Getter 方法
    public String getBlockHash() {
        return blockHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public BlockData getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}