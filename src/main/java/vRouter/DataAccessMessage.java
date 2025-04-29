package vRouter;

import java.math.BigInteger;
import java.util.*;


public class DataAccessMessage {
    private static final int BIGINT_SIZE = 32;      // BigInteger估算大小
    private static final int DOUBLE_SIZE = 8;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;
    private static final int STRING_SIZE = 40;      // 平均字符串长度估算
    private static final int HASH_SIZE = 32;        // 哈希值长度
    private static final int MAP_ENTRY_OVERHEAD = 16; // Map每项额外开销
    private static final int LIST_ENTRY_OVERHEAD = 12; // List每项额外开销
    private static final int VRF_OUTPUT_SIZE = 64;
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
    public int getSize() {
        int size = 0;

        // 基础字段
        size += BIGINT_SIZE;    // from
        size += DOUBLE_SIZE;    // nodeScore
        size += INT_SIZE;       // accessCount
        size += INT_SIZE;       // uniqueAccessNodes
        size += BIGINT_SIZE;    // input
        size += VRF_OUTPUT_SIZE; // vrfoutput
        size += LONG_SIZE;      // cycle
        size += HASH_SIZE;      // merkleRoot

        // dataAccessCount Map
        for (Map.Entry<String, Integer> entry : dataAccessCount.entrySet()) {
            size += STRING_SIZE + INT_SIZE + MAP_ENTRY_OVERHEAD;
        }

        // dataAccessNodes Map
        for (Map.Entry<String, Set<String>> entry : dataAccessNodes.entrySet()) {
            size += STRING_SIZE + MAP_ENTRY_OVERHEAD;
            for (String node : entry.getValue()) {
                size += STRING_SIZE + LIST_ENTRY_OVERHEAD;
            }
        }

        // recordHashes List
        size += recordHashes.size() * (HASH_SIZE + LIST_ENTRY_OVERHEAD);

        // proofPaths Map
        for (Map.Entry<String, List<String>> entry : proofPaths.entrySet()) {
            size += HASH_SIZE + MAP_ENTRY_OVERHEAD;
            for (String proof : entry.getValue()) {
                size += HASH_SIZE + LIST_ENTRY_OVERHEAD;
            }
        }

        return size;
    }

}