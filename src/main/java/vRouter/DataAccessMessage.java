package vRouter;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class DataAccessMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public final BigInteger from;
    public final BigInteger to;
    public final double nodeScore;
    public final int accessCount;
    public final int uniqueAccessNodes;

    public final Map<String, Integer> dataAccessCount;
    public final Map<String, Set<String>> dataAccessNodes;

    public final String merkleRoot;
    public final byte[] input;
    public final VRFElection.VRFOutput vrfoutput;
    public final long cycle;
    public transient int cachedSize = -1;

    public DataAccessMessage(BigInteger senderId,
                             BigInteger to,
                             double nodeScore,
                             int accessCount,
                             int uniqueAccessNodes,
                             Map<String, Integer> dataAccessCount,
                             Map<String, Set<String>> dataAccessNodes,
                             List<AccessRecord> accessRecords,
                             byte[] input,
                             VRFElection.VRFOutput vrfoutput,
                             long currentCycle) {
        this.from = senderId;
        this.to=to;
        this.nodeScore = nodeScore;
        this.accessCount = accessCount;
        this.uniqueAccessNodes = uniqueAccessNodes;
        this.dataAccessCount = dataAccessCount;
        this.dataAccessNodes = dataAccessNodes;
        this.input = input;
        this.vrfoutput = vrfoutput;
        this.cycle = currentCycle;

        this.cachedSize=getSize();

        // 构建默克尔树
        List<String> recordHashes = new ArrayList<>();
        for (AccessRecord record : accessRecords) {
            recordHashes.add(record.getHash());
        }
        MerkleTree merkleTree = new MerkleTree(recordHashes);
        this.merkleRoot = merkleTree.getRootHash();
    }
    public int getSize() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this); // 序列化整个对象
            oos.flush();
            cachedSize = baos.toByteArray().length;
            return cachedSize;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
