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
        public boolean recycled;
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
        if (turns >= dataGenerateSimCycle) {
            // 数据生成阶段结束
            if (turns == dataGenerateSimCycle) {
                // 只在第一次到达时初始化排名
                QueryGenerator.initializeGlobalRanking();
            }
            return false;
        }

        for (int i = 0; i < dataPerCycle; i++) {
            Node start = getRandomUpNode();
            if (start == null) return false;

            VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);

            // 前56位二进制字符串
            String prefix56 = new BigInteger(56, CommonState.r).toString(2);
            prefix56 = String.format("%56s", prefix56).replace(' ', '0');

            // 固定的 "00"
            String fixed00 = "00";

            // 随机生成后缀部分
            int remainingBits = totalBits - 58;
            String randomSuffix = new BigInteger(remainingBits, CommonState.r).toString(2);
            randomSuffix = String.format("%" + remainingBits + "s", randomSuffix).replace(' ', '0');

            // 拼接完整的二进制字符串
            String fullBinary = prefix56 + fixed00 + randomSuffix;

            // 转换为 BigInteger
            BigInteger dataID = new BigInteger(fullBinary, 2);

            // 将前缀56位转为十六进制字符串作为 prefix 标识
            BigInteger prefixBigInt = new BigInteger(prefix56, 2);

            DataInfo info = new DataInfo();
            info.dataId = dataID;
            info.prefix = prefixBigInt.toString(16);
            info.recycled = false;

            QueryGenerator.dataRegistry.put(dataID, info);
            QueryGenerator.dataPrefixes.add(prefixBigInt.toString(16));
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