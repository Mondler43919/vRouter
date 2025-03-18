package vRouter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BlockData {
    private String rootHash; // 默克尔树根哈希
    private BigInteger newCentralNodeId; // 新中心节点 ID
    private List<BigInteger> candidateList; // 候选节点集合
    private HashMap<BigInteger, double[]> dataScores; // 数据评分
    private HashMap<BigInteger, Double> nodeScores; // 节点评分
    private HashMap<BigInteger, Object> nodeMetrics; // 节点评分依据

    // 构造方法
    public BlockData(String rootHash,
                     BigInteger newCentralNodeId,
                     List<BigInteger> candidateList,
                     HashMap<BigInteger, double[]> dataScores,
                     HashMap<BigInteger, Double> nodeScores,
                     HashMap<BigInteger, Object> nodeMetrics) {
        this.rootHash = rootHash;
        this.newCentralNodeId = newCentralNodeId;
        this.candidateList = candidateList;
        this.dataScores = dataScores;
        this.nodeScores = nodeScores;
        this.nodeMetrics = nodeMetrics;
    }

    // Getter 方法
    public String getRootHash() {
        return rootHash;
    }

    public BigInteger getNewCentralNodeId() {
        return newCentralNodeId;
    }

    public List<BigInteger> getCandidateSet() {
        return candidateList;
    }

    public HashMap<BigInteger, double[]> getDataScores() {
        return dataScores;
    }


    public HashMap<BigInteger, Double> getNodeScores() {
        return nodeScores;
    }

    public HashMap<BigInteger, Object> getNodeMetrics() {
        return nodeMetrics;
    }

    @Override
    public String toString() {
        return "BlockData{" +
                "rootHash='" + rootHash + '\'' +
                ", newCentralNodeId=" + newCentralNodeId +
                ", candidateSet=" + candidateList +
                ", dataScores=" + dataScores +
                ", nodeScores=" + nodeScores +
                ", nodeMetrics=" + nodeMetrics +
                '}';
    }
}
