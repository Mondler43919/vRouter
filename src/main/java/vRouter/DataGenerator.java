package vRouter;

import kademlia.*;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Control;

import java.math.BigInteger;

public class DataGenerator implements Control {
    private final static String PAR_PROT = "protocol";
    private final static String TURNS = "turns";
    private final static String CYCLES = "cycles";
    private final static String DATA_PER_CYCLE_PARAM = "DATA_PER_CYCLE";
    private final int pid;
    private int dataGenerateSimCycle;
    private int totalSimCycle;
    private int dataPerCycle; // 每周期生成数据量
    private int turns = 0;

    public DataGenerator(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        dataGenerateSimCycle = Configuration.getInt(prefix + "." + TURNS);
        totalSimCycle = Configuration.getInt(prefix + "." + CYCLES);
        dataPerCycle = Configuration.getInt(prefix + "." + DATA_PER_CYCLE_PARAM, 100); // 默认100
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
            Node start;
            do {
                start = Network.get(CommonState.r.nextInt(Network.size()));
            } while ((start == null) || (!start.isUp()));

            VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);
            BigInteger nodeID = p.nodeId;
            int totalBits = KademliaCommonConfig.BITS;

            // 获取节点ID的前56位
            String nodeBinary = String.format("%" + totalBits + "s", nodeID.toString(2)).replace(' ', '0');
            String prefix56 = nodeBinary.substring(0, 56);

            // 固定的2位
            String fixed00 = "00";

            // 剩余位数
            int remainingBits = totalBits - 58;

            // 生成剩余部分的随机二进制字符串
            String randomSuffix = new BigInteger(remainingBits, CommonState.r).toString(2);
            randomSuffix = String.format("%" + remainingBits + "s", randomSuffix).replace(' ', '0');

            // 拼接完整的二进制字符串
            String fullBinary = prefix56 + fixed00 + randomSuffix;

            // 转换成 BigInteger 数据ID
            BigInteger dataID = new BigInteger(fullBinary, 2);

            // 存储数据
            QueryGenerator.availableDataList.add(dataID);
            VRouterObserver.dataIndexTraffic.put(dataID, 0);
            p.storeData(dataID, pid);
        }
        return false;
    }
}