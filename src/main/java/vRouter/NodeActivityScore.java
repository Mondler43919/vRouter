package vRouter;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

public class NodeActivityScore {

    // 权重系数，可以根据实际情况调整
    private static final double WEIGHT_ACCESS_COUNT = 0.4;
    private static final double WEIGHT_UNIQUE_NODE_COUNT = 0.3;
    private static final double WEIGHT_ENTROPY = 0.3;

    // 计算活跃度评分
    public static double calculateActivityScore(Map<String, Object> dataMetrics) {
        Integer accessCount = (Integer) dataMetrics.get("accessCount");
        Integer uniqueAccessNodes = (Integer) dataMetrics.get("uniqueAccessNodes");
        Map<BigInteger, Integer> dataAccessCount = (Map<BigInteger, Integer>)dataMetrics.get("dataAccessCount");
        double entropy = calculateEntropy(dataAccessCount,accessCount);

        // 被访问次数的对数压缩
        double logAccessCount = Math.log1p(accessCount);  // log(x+1) 避免对数为负
        // 被访问的独立节点数对数压缩
        double logUniqueNodeCount = Math.log1p(uniqueAccessNodes);  // log(x+1) 避免对数为负

        // 计算总的活跃度评分
        return WEIGHT_ACCESS_COUNT * logAccessCount +
                WEIGHT_UNIQUE_NODE_COUNT * logUniqueNodeCount +
                WEIGHT_ENTROPY * entropy;
    }

    // 计算熵值
    public static double calculateEntropy(Map<BigInteger, Integer> requestCounts,Integer accessCount) {
        double entropy = 0.0;
        for (int count : requestCounts.values()) {
            double probability = (double) count /accessCount;
            entropy -= probability * Math.log(probability);
        }

        return entropy;
    }
}
