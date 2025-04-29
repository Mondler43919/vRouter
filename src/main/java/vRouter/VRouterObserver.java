package vRouter;

import com.google.gson.Gson;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class VRouterObserver implements Control {
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

    // 新增区块链指标
    public static final AtomicLong lastBlockCreatedTime = new AtomicLong(0);
    public static final AtomicInteger currentBlockTxCount = new AtomicInteger(0);

    // 新增网络性能指标
    public static final AtomicLong totalBytesTransferred = new AtomicLong(0);
    public static final IncrementalStats bandwidthStats = new IncrementalStats();

    // 回收统计指标
    public static AtomicInteger totalData = new AtomicInteger(0);               // 总数据量（可选）
    public static AtomicInteger totalActive = new AtomicInteger(0);             // 活跃数据量
    public static AtomicInteger recycledActive = new AtomicInteger(0);          // 活跃被回收数
    public static AtomicInteger recycledInactive = new AtomicInteger(0);        // 不活跃被回收数


    private static final String PAR_PROT = "protocol";
    private int pid;
    private String prefix;
    private long lastStatTime = 0;

    public VRouterObserver(String prefix) {
        this.prefix = prefix;
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }

    public boolean execute() {
        long now = CommonState.getTime();
        int currentCycle = (int)(now / 1000); // 假设每个周期1000ms

        // 常规统计指标
        double blockInterval = (now - lastBlockCreatedTime.get())/1000.0;
        double tps = calculateTPS(now);
        double throughput = calculateThroughput(now);

        // 最后一个周期计算回收指标
        int totalCycles = Configuration.getInt("CYCLES");
        if (currentCycle >= totalCycles - 1) {
            calculateFinalRecycleMetrics();
        }

        // 常规输出
        String output = String.format("TPS=%.1f | Block=%.2fs | BW=%.2fkB/s",
                tps, blockInterval, throughput);
        System.err.println(output);

        // 重置周期统计量
        if(now - lastStatTime >= 1000) {
            resetPeriodicStats();
            lastStatTime = now;
        }

        return false;
    }

    // 在VRouterObserver类中修改calculateFinalRecycleMetrics方法
    private void calculateFinalRecycleMetrics() {
        int activeRecycled = recycledActive.get();
        int activeTotal = totalActive.get();
        int activeNotRecycled = activeTotal - activeRecycled;

        int inactiveRecycled = recycledInactive.get();
        int inactiveTotal = totalData.get() - activeTotal;
        int inactiveNotRecycled = inactiveTotal - inactiveRecycled;


        System.err.println("\n====== RECYCLE CONFUSION MATRIX ======");
        System.err.println("+---------------------+------------+---------------+");
        System.err.println("|                     | Recycled   | Not Recycled  |");
        System.err.println("+---------------------+------------+---------------+");
        System.err.printf("| LONG_TERM_ACTIVE    | %-10d | %-13d |\n", activeRecycled, activeNotRecycled);
        System.err.println("+---------------------+------------+---------------+");
        System.err.printf("| LONG_TERM_INACTIVE  | %-10d | %-13d |\n", inactiveRecycled, inactiveNotRecycled);
        System.err.println("+---------------------+------------+---------------+");
        System.err.println(QueryGenerator.dataPrefixes);
    }


    // public static void recordDataRecycle(BigInteger dataId, DataGenerator.DataInfo info) {
    //     DataGenerator.DataType type=info.type;
    //     if (type == DataGenerator.DataType.LONG_TERM_ACTIVE) {
    //         recycledActive.incrementAndGet();
    //     } else if (type == DataGenerator.DataType.LONG_TERM_INACTIVE) {
    //         recycledInactive.incrementAndGet();
    //     }
    //     System.err.println("prefix:"+info.prefix+"  dataId:"+dataId+"   type:"+type);
    // }


    private double calculateTPS(long currentTime) {
        return currentBlockTxCount.get() /
                ((currentTime - lastStatTime)/1000.0 + 0.001); // 避免除零
    }

    private double calculateThroughput(long currentTime) {
        long bytes = totalBytesTransferred.get();
        double seconds = (currentTime - lastStatTime)/1000.0;
        return (bytes/(1024.0)) / (seconds + 0.001);
    }
    private void resetPeriodicStats() {
        currentBlockTxCount.set(0);
        totalBytesTransferred.set(0);
        bandwidthStats.reset();
    }

}