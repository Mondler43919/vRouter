package vRouter;

import java.util.Date;
import java.io.*;

public class Block implements Serializable {
    private static final long serialVersionUID = 1L;

    private String blockHash;
    private String previousHash;
    private BlockData data;
    private long timestamp;
    public transient int cachedSize = -1;

    public Block(String previousHash, BlockData data, long timestamp) {
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
        this.blockHash = calculateHash();
        this.cachedSize=calculateBlockSize(this);
    }

    public Block(String previousHash, BlockData data) {
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = new Date().getTime();
        this.blockHash = calculateHash();
    }

    public String calculateHash() {
        String input = previousHash + data.toString() + timestamp;
        return HashUtils.SHA256(input);
    }

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

    public static int calculateBlockSize(Block block) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(block);
            oos.flush();
            return baos.toByteArray().length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
