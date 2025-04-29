package vRouter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import peersim.core.CommonState;

public class DataActivityScore {

    // 计算所有数据的活跃度评分
    public static HashMap<String, double[]> calculateActivityScore(
            Map<String, Integer> dataAccessCounts,  // 数据访问次数
            Map<String, Set<String>> dataAccessNodes,  // 访问的独立节点
            Map<String, int[][]> historyData,  // 历史活跃度记录（访问次数和活跃度等级）
            HashMap<String, double[]> previousCycleMetrics
            ){ // 上一周期的评分信息

        double weightAccessCount=0.7; // 访问次数的权重
        double weightUniqueAccessNodes=0.2;// 独立访问节点的权重
        double weightstability=0.1; // 稳定性评分权重
        double[] activityThresholds={0.15, 0.20, 0.40, 0.82, 0.92};  // 活跃度等级阈值
        double a = 0.3; // 平滑系数 (a)

        // 活跃度评分结果
        HashMap<String, double[]> activityMetrics = new HashMap<>();
        HashMap<String, Double> scoreCache = new HashMap<>();
        double maxScore = 0.0;
        double threshold2 = activityThresholds[2]; // ʘ2 作为初始活跃度判断标准

        // 单次遍历：计算score并记录maxScore
        for (Map.Entry<String, Integer> entry : dataAccessCounts.entrySet()) {
            String dataId = entry.getKey();
            int accessCount = entry.getValue();
            int uniqueAccessNodes = dataAccessNodes.getOrDefault(dataId, new HashSet<>()).size();

            // 获取历史数据
            int[] accessCountsHistory = historyData.getOrDefault(dataId, new int[2][10])[0]; // 过去 10 周期的访问次数

            // 计算历史访问次数的稳定性
            double stability = calculateStability(accessCountsHistory);

            // 计算 `score`，考虑稳定性
            double score = weightAccessCount * accessCount + weightUniqueAccessNodes * uniqueAccessNodes + weightstability*stability;

            // 记录 `score`，避免二次计算
            scoreCache.put(dataId, score);

            // 更新最大值
            if (score > maxScore) {
                maxScore = score;
            }
        }

        // 二次遍历：计算 活跃度等级 是否不活跃 平滑评分
        for (Map.Entry<String, Double> entry : scoreCache.entrySet()) {
            String dataId = entry.getKey();
            double score = entry.getValue();
            int accessCount = dataAccessCounts.get(dataId);
            int uniqueAccessNodes = dataAccessNodes.getOrDefault(dataId, new HashSet<>()).size();

            // 计算 `activityLevel`
            int activityLevel = 0;
            for (int i = 0; i < activityThresholds.length; i++) {
                if (score >= activityThresholds[i] * maxScore) {
                    activityLevel = i;
                } else {
                    break;
                }
            }

            // 判断是否“不活跃
            boolean inactive = false;
            if (score < threshold2 * maxScore) { // 低于 ʘ2 阈值，可能不活跃
                int[] historyLevels = historyData.getOrDefault(dataId, new int[2][10])[1]; // 取历史活跃度等级
                int maxHistoryLevel = 0;
                for (int level : historyLevels) {
                    if (level > maxHistoryLevel) {
                        maxHistoryLevel = level;
                    }
                }
                if (maxHistoryLevel < 4) { // 历史最高活跃度等级小于 4，标记为“不活跃”
                    inactive = true;
                }
            }

            // 计算平滑评分
            double previousScore = 0.0;
            if (previousCycleMetrics.containsKey(dataId)) {
                // 上一周期评分
                previousScore = previousCycleMetrics.get(dataId)[0];  // 取上一周期的评分（平滑后的评分）
            }

            // 计算平滑评分：`a * 上周期评分 + (1 - a) * 当前评分`
            double smoothedScore = a * previousScore + (1 - a) * score;

            double[] dataMetrics = new double[5];
            dataMetrics[0] = smoothedScore;  // 平滑后的评分
            dataMetrics[1] = accessCount;   // 访问次数
            dataMetrics[2] = uniqueAccessNodes; // 独立访问节点数
            dataMetrics[3] = activityLevel;   // 活跃度等级
            dataMetrics[4] = (inactive && CommonState.getTime()/1000 >= 123) ? 0 : 1; // 活跃状态：0 - 不活跃, 1 - 活跃

            activityMetrics.put(dataId, dataMetrics);
        }

        return activityMetrics;
    }

    // 计算历史访问次数的稳定性：1 - (历史均值 / 标准差)
    private static double calculateStability(int[] accessCountsHistory) {
        if (accessCountsHistory == null || accessCountsHistory.length == 0) {
            return 0.0;
        }

        // 计算均值
        double sum = 0;
        for (int count : accessCountsHistory) {
            sum += count;
        }
        double mean = sum / accessCountsHistory.length;

        // 计算标准差
        double variance = 0;
        for (int count : accessCountsHistory) {
            variance += Math.pow(count - mean, 2);
        }
        variance /= accessCountsHistory.length;
        double stddev = Math.sqrt(variance);

        // 稳定性 = 1 - (均值 / 标准差)
        if (stddev == 0) {
            return 0.0;  // 如果标准差为 0，则表示稳定性为 0（即完全没有波动）
        }
        return 1 - (mean / stddev);
    }
}


