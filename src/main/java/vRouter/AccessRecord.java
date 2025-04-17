package vRouter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AccessRecord {
    private long timestamp;
    private BigInteger nodeId;
    private BigInteger from;
    private BigInteger dataId;
    private String hash;

    public AccessRecord(long timestamp, BigInteger nodeId, BigInteger from, BigInteger dataId,String hash) {
        this.timestamp = timestamp;
        this.nodeId = nodeId;
        this.from = from;
        this.dataId = dataId;
        this.hash = hash;
    }
    // 生成记录的唯一字符串表示
    public String toHashString() {
        return timestamp + "|" + nodeId + "|" + from + "|" + dataId;
    }

    // Getters
    public String getHash() { return hash; }
    public long getTimestamp() { return timestamp; }
    public BigInteger getNodeId() { return nodeId; }
    public BigInteger getFrom() { return from; }
    public BigInteger getDataId() { return dataId; }
}