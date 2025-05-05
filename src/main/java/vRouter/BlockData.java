package vRouter;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class BlockData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String globalMerkleRoot;
    private final String centralNodeId;
    private final Map<String, BigInteger> candidateMap;
    private final Map<String, double[]> dataScores;
    private final Map<String, Double> nodeScores;

    public BlockData(String globalMerkleRoot,
                     String centralNodeId,
                     Map<String, BigInteger> candidateMap,
                     Map<String, double[]> dataScores,
                     Map<String, Double> nodeScores) {

        this.globalMerkleRoot = globalMerkleRoot;
        this.centralNodeId = centralNodeId;
        this.candidateMap = new HashMap<>(candidateMap);

        this.dataScores = new HashMap<>();
        dataScores.forEach((k, v) -> this.dataScores.put(k, v.clone()));

        this.nodeScores = new HashMap<>(nodeScores);
    }

    public String getGlobalMerkleRoot() {
        return globalMerkleRoot;
    }

    public String getCentralNodeId() {
        return centralNodeId;
    }

    public Map<String, BigInteger> getCandidateSet() {
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


    public String getNodeMerkleRoot() {
        return globalMerkleRoot;
    }
}
