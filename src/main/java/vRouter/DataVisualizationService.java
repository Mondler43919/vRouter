package vRouter;

import org.knowm.xchart.XYSeries;
import peersim.core.CommonState;
import peersim.core.Control;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataVisualizationService implements Control {
    private static DataVisualizationService instance; // 静态单例

    private final Map<String, List<Object[]>> nodeStats = new ConcurrentHashMap<>();

    public DataVisualizationService(String prefix) {
        if (instance != null) {
            throw new RuntimeException("DataVisualizationService already initialized!");
        }
        instance = this;
    }
    public static DataVisualizationService getInstance(){
        return instance;
    }
    public synchronized void updatePerRound(
            long round,
            Map<String, Integer> accessCounts,
            Map<String, Set<String>> accessNodes,
            Map<String, double[]> dataScores) {

        accessCounts.forEach((dataId, accessCount) -> {
            int nodeCount = accessNodes.getOrDefault(dataId, Collections.emptySet()).size();
            if(dataScores.containsKey(dataId)){
                double score = dataScores.get(dataId)[0];
                int recycled =(int) dataScores.get(dataId)[4];
                int level = (int) dataScores.get(dataId)[3];
                ExcelLogger.SimpleLogger.logSimpleAccess(round, dataId, accessCount, nodeCount, score,recycled,level);
            }
        });
    }
    @Override
    public boolean execute() {
        return false;
    }
}