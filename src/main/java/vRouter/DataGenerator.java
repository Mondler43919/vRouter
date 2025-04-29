package vRouter;

import kademlia.*;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Control;

import java.math.BigInteger;
import java.util.Random;
import org.apache.commons.math3.distribution.ZipfDistribution;
public class DataGenerator implements Control {
    private final static String PAR_PROT = "protocol";
    private final static String TURNS = "turns";
    private final static String CYCLES = "cycles";
    private final static String DATA_PER_CYCLE_PARAM = "DATA_PER_CYCLE";
    private final int  totalBits = Configuration.getInt("BITS");
    private final int pid;
    private int dataGenerateSimCycle;
    private int totalSimCycle;
    private int dataPerCycle;
    private int turns = 0;
    private final Random random = new Random();
    private ZipfDistribution zipf;

    // 幂律参数
    private static final double POWER_LAW_EXPONENT = 2.0; // 幂律指数
    private static final double MIN_ACTIVITY = 1.0; // 最小活跃度

    public static class DataInfo {
        public BigInteger dataId;
        public String prefix;
        public double popularity; // 当前流行度
        public boolean recycled;
        public long lastActiveTime; // 上次活跃时间
        public long activeDuration; // 活跃持续时间
        public long inactiveDuration; // 不活跃持续时间
        public double historicalPopularity;
        public double peakPopularity;
        public long lastSurgeTime;
        public int currentRank;
    }

    public DataGenerator(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        dataGenerateSimCycle = Configuration.getInt(prefix + "." + TURNS);
        totalSimCycle = Configuration.getInt(prefix + "." + CYCLES);
        dataPerCycle = Configuration.getInt(DATA_PER_CYCLE_PARAM, 100);
        // 初始化Zipf分布
        zipf = new ZipfDistribution(dataPerCycle, POWER_LAW_EXPONENT);
    }

    public boolean execute() {
        turns++;
        if (turns >= dataGenerateSimCycle + 20) {
            if (turns + 20 >= totalSimCycle) {
                QueryGenerator.executeFlag = false;
                return false;
            }
            QueryGenerator.executeFlag = true;
            return false;
        }

        if (turns >= dataGenerateSimCycle) return false;

        for (int i = 0; i < dataPerCycle; i++) {
            Node start = getRandomUpNode();
            if (start == null) return false;

            VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);
            BigInteger nodeID = p.nodeId;

            String nodeBinary = String.format("%" + totalBits + "s", nodeID.toString(2)).replace(' ', '0');
            String prefix56 = nodeBinary.substring(0, 56);
            BigInteger prefixBigInt = new BigInteger(prefix56, 2);

            String fixed00 = "00";
            int remainingBits = totalBits - 58;
            String randomSuffix = new BigInteger(remainingBits, CommonState.r).toString(2);
            randomSuffix = String.format("%" + remainingBits + "s", randomSuffix).replace(' ', '0');

            String fullBinary = prefix56 + fixed00 + randomSuffix;
            BigInteger dataID = new BigInteger(fullBinary, 2);

            DataInfo info = new DataInfo();
            info.dataId = dataID;
            info.prefix = prefixBigInt.toString(16);
            info.recycled = false;
            info.lastActiveTime = CommonState.getTime();

            // 使用Zipf分布设置初始活跃度
            info.popularity = MIN_ACTIVITY + zipf.sample();

            QueryGenerator.dataRegistry.put(dataID, info);
            QueryGenerator.dataPrefixes.add(prefixBigInt.toString(16));
            VRouterObserver.totalData.incrementAndGet();
            p.storeData(dataID, pid);
        }
        return false;
    }

    private Node getRandomUpNode() {
        int attempts = 0;
        while (attempts < Network.size()) {
            Node node = Network.get(CommonState.r.nextInt(Network.size()));
            if (node != null && node.isUp()) {
                return node;
            }
            attempts++;
        }
        return null;
    }
}