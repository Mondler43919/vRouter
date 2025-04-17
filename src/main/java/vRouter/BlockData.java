package vRouter;

import java.math.BigInteger;
import java.util.*;

public class BlockData {
    private final String globalMerkleRoot;      // 全局默克尔根
    private final String centralNodeId;     // 中心节点ID
    private final Map<String, BigInteger> candidateMap; // 候选节点集合
    private final Map<String, double[]> dataScores; // 数据评分
    private final Map<String, Double> nodeScores;   // 节点评分
    private final Map<String, String> nodeProofs;   // 新增：各节点的默克尔根证明

    public BlockData(String globalMerkleRoot,
                     String centralNodeId,
                     Map<String, BigInteger> candidateMap,
                     Map<String, double[]> dataScores,
                     Map<String, Double> nodeScores,
                     Map<String, String> stringNodeProofs) {  // 接收String键的证明

        this.globalMerkleRoot = globalMerkleRoot;
        this.centralNodeId = centralNodeId;
        this.candidateMap =new HashMap<>(candidateMap);

        // 防御性拷贝
        this.dataScores = new HashMap<>();
        dataScores.forEach((k, v) -> this.dataScores.put(k, v.clone()));

        this.nodeScores = new HashMap<>(nodeScores);

        // 转换String键到BigInteger
        this.nodeProofs = new HashMap<>(stringNodeProofs);
    }

    // ========== Getters ==========
    public String getGlobalMerkleRoot() {
        return globalMerkleRoot;
    }

    public String getCentralNodeId() {
        return centralNodeId;
    }

    public Map<String,BigInteger> getCandidateSet() {
        return candidateMap;
    }

    public HashMap<String, double[]> getDataScores() {
        HashMap<String, double[]> result = new HashMap<>();
        dataScores.forEach((k, v) -> result.put(k, v.clone()));
        return result;
    }

    public Map<String, Double> getNodeScores() {
        return new HashMap<>(nodeScores);
    }

    // ========== 新增方法 ==========
    public Map<String, String> getNodeProofs() {
        return new HashMap<>(nodeProofs);
    }

    /**
     * 获取指定节点的默克尔根（用于验证）
     */
    public String getNodeMerkleRoot(String nodeId) {
        return nodeProofs.get(nodeId);
    }

    /**
     * 验证数据是否存在于指定节点的默克尔树中
     * @param nodeId 节点ID
     * @param recordHash 记录哈希
     * @param path 该记录在节点默克尔树中的路径
     */
    public boolean verifyNodeRecord(String nodeId, String recordHash, List<String> path) {
        String nodeRoot = nodeProofs.get(nodeId);
        return nodeRoot != null &&
                MerkleTree.verify(recordHash, path, nodeRoot);
    }
}