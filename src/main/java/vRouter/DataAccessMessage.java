package vRouter;

import java.math.BigInteger;
import java.util.*;

public class DataAccessMessage {
    public final BigInteger from;
    public final double nodeScore;
    public final int accessCount;
    public final int uniqueAccessNodes;

    public final Map<String, Integer> dataAccessCount;
    public final Map<String, Set<String>> dataAccessNodes;

    // 默克尔证明相关字段
    public final String merkleRoot;          // 本节点记录的默克尔根
    public final List<String> recordHashes;  // 所有记录的哈希列表
    public final Map<String, List<String>> proofPaths; // <记录哈希, 验证路径>
    public final BigInteger input;
    public final VRFElection.VRFOutput vrfoutput;
    public final long cycle;

    public DataAccessMessage(BigInteger senderId,
                             double nodeScore,
                             int accessCount,
                             int uniqueAccessNodes,
                             Map<String, Integer> dataAccessCount,
                             Map<String, Set<String>> dataAccessNodes,
                             List<AccessRecord> accessRecords,
                             BigInteger input,
                             VRFElection.VRFOutput vrfoutput,
                             long currentCycle) {
        this.from = senderId;
        this.nodeScore = nodeScore;
        this.accessCount = accessCount;
        this.uniqueAccessNodes = uniqueAccessNodes;
        this.dataAccessCount = dataAccessCount;
        this.dataAccessNodes = dataAccessNodes;
        this.input=input;
        this.vrfoutput=vrfoutput;
        this.cycle=currentCycle;

        // 构建默克尔树
        this.recordHashes = new ArrayList<>();
        this.proofPaths = new HashMap<>();

        // 提取所有记录的哈希
        for (AccessRecord record : accessRecords) {
            recordHashes.add(record.getHash());
        }

        // 生成默克尔树和验证路径
        MerkleTree merkleTree = new MerkleTree(recordHashes);
        this.merkleRoot = merkleTree.getRootHash();

        // 为每条记录生成验证路径
        for (int i = 0; i < recordHashes.size(); i++) {
            proofPaths.put(recordHashes.get(i), merkleTree.getProofPath(i));
        }
    }

}