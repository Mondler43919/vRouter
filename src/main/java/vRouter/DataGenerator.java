package vRouter;

import kademlia.*;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Control;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class DataGenerator implements Control {
    private final static String PAR_PROT = "protocol";
    private final static String TURNS = "turns";
    private final static String CYCLES = "cycles";
    private final static String DATA_PER_CYCLE_PARAM = "DATA_PER_CYCLE";
    private final int pid;
    private int dataGenerateSimCycle;
    private int totalSimCycle;
    private int dataPerCycle; // 每周期生成数据量
    private UniformRandomGenerator urg;
    private int turns = 0;

    public DataGenerator(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        dataGenerateSimCycle = Configuration.getInt(prefix + "." + TURNS);
        totalSimCycle = Configuration.getInt(prefix + "." + CYCLES);
        dataPerCycle = Configuration.getInt(prefix + "." + DATA_PER_CYCLE_PARAM, 100); // 默认100
        urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
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

        // 生成可配置数量的数据
        for (int i = 0; i < dataPerCycle; i++) {
            Node start;
            do {
                start = Network.get(CommonState.r.nextInt(Network.size()));
            } while ((start == null) || (!start.isUp()));

            VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);
            BigInteger dataID = urg.generate();
            QueryGenerator.availableDataList.add(dataID); // 添加到List
            VRouterObserver.dataIndexTraffic.put(dataID, 0);
            p.storeData(dataID, pid);
        }
        return false;
    }
}