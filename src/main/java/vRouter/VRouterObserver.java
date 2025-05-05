package vRouter;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class VRouterObserver implements Control {
    public double cycleLength=(double) Configuration.getInt("CYCLE");
    // 原有查找统计指标
    public static IncrementalStats successLookupForwardHop = new IncrementalStats();
    public static IncrementalStats successLookupBackwardHop = new IncrementalStats();
    public static IncrementalStats totalSuccessHops = new IncrementalStats();
    public static IncrementalStats failedBackwardLookupHop = new IncrementalStats();
    public static IncrementalStats indexHop = new IncrementalStats();
    public static IncrementalStats bloomFilterCount = new IncrementalStats();
    public static IncrementalStats latencyStats = new IncrementalStats();
    public static HashMap<BigInteger, Integer> dataIndexTraffic = new HashMap<>();
    public static HashMap<BigInteger, Integer> dataQueryTraffic = new HashMap<>();

    // 区块链指标
    public static final ConcurrentHashMap<String, Long> blockCreationTimes = new ConcurrentHashMap<>();
    public static final IncrementalStats blockPropagationTime = new IncrementalStats();
    public static final AtomicInteger blocksReceivedCount = new AtomicInteger(0);
    public static final int REQUIRED_CONFIRMATIONS = (int) (Network.size() * 0.67); // 2/3节点数
    public static final AtomicInteger currentBlockTxCount = new AtomicInteger(0);

    // 网络性能指标
    public static final AtomicLong totalBytesTransferred = new AtomicLong(0);
    private static final ArrayList<Double> bandwidthHistory = new ArrayList<>();
    private static final String PAR_PROT = "protocol";
    private int pid;
    private String prefix;
    private long lastStatTime = 0;
    private int totalcycle=Configuration.getInt("CYCLES");

    public VRouterObserver(String prefix) {
        this.prefix = prefix;
        pid = Configuration.getPid(prefix + "." + PAR_PROT);

    }

    public boolean execute() {
        long now = CommonState.getTime();
        int currentCycle = (int)(now / cycleLength);

        // 常规统计指标
        double tps = calculateTPS(now);
        double throughput = calculateThroughput(now);

        if(currentCycle>120 && currentCycle<totalcycle-20){
            bandwidthHistory.add(throughput);
        }
        if(currentCycle==totalcycle-1)
        {
            double averageBW= bandwidthHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double averagePropagationTime = blockPropagationTime.getAverage()/Configuration.getInt("CYCLE");
            String output1=String.format("平均BW=%.3f  |  平均产块时间=%.4s：",averageBW,averagePropagationTime);
            System.err.println(output1);
        }

        // 常规输出
        String output = String.format("TPS=%.1f | BW=%.2fMB/s", tps, throughput);
        System.err.println(output);

        // 重置周期统计量
        resetPeriodicStats();
        lastStatTime = now;

        return false;
    }

    private double calculateTPS(long currentTime) {
        return currentBlockTxCount.get() /
                ((currentTime - lastStatTime)/cycleLength + 0.001); // 避免除零
    }

    private double calculateThroughput(long currentTime) {
        long bytes = totalBytesTransferred.get();
        double seconds = (currentTime - lastStatTime)/cycleLength;
        return (bytes/(1024.0*1024.0)) / (seconds + 0.001);
    }
    private void resetPeriodicStats() {
        currentBlockTxCount.set(0);
        totalBytesTransferred.set(0);
    }
}