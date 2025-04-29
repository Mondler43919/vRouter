package vRouter;

import kademlia.KademliaCommonConfig;
import kademlia.UniformRandomGenerator;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;

import java.math.BigInteger;
import java.util.*;

public class QueryGenerator implements Control {
    // 论文启发的常量配置
    private static final double POWER_LAW_EXPONENT = 1.5; // 论文测得的排名变化指数
    private static final int TOTAL_QUERIES_PER_CYCLE = 1000;
    private static final double EXTERNAL_EVENT_PROB = 0.3; // 论文中的外部事件概率
    private static final double TOP_TIER_DECAY = 0.95; // Top10衰减率
    private static final double BASE_DECAY = 0.85; // 基础衰减率

    public static boolean executeFlag = false;
    public static HashMap<BigInteger, DataGenerator.DataInfo> dataRegistry = new HashMap<>();
    public static Set<String> dataPrefixes = new HashSet<>();
    public static HashMap<BigInteger, Integer> queriedData = new HashMap<>();

    private final int pid;
    private final UniformRandomGenerator urg;
    private final Random random = CommonState.r;
    private final RankingEngine rankingEngine = new RankingEngine();

    public QueryGenerator(String prefix) {
        pid = Configuration.getPid(prefix + ".protocol");
        urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, random);
    }

    public boolean execute() {
        if (!executeFlag || dataRegistry.isEmpty()) return false;

        long currentCycle = CommonState.getTime() / Configuration.getInt("CYCLE");

        // 论文启发的更新流程
        updateRankings(currentCycle);
        distributeQueries();

        return false;
    }

    // 论文式排名更新（第4章）
    private void updateRankings(long currentCycle) {
        List<DataGenerator.DataInfo> activeItems = new ArrayList<>();

        for (DataGenerator.DataInfo info : dataRegistry.values()) {
            // 分层衰减处理（论文4.2节）
            applyDecay(info);

            // 状态转换（论文图4）
            if (info.popularity > 0) {
                activeItems.add(info);
                updateActiveState(info, currentCycle);
            } else {
                updateInactiveState(info, currentCycle);
            }
        }

        // 更新排名（论文图3）
        rankingEngine.updateRanks(activeItems);
    }

    // 论文式衰减模型（公式3）
    private void applyDecay(DataGenerator.DataInfo info) {
        if (rankingEngine.getRank(info.dataId) < 10) {
            info.popularity *= TOP_TIER_DECAY; // Top10衰减慢
        } else {
            info.popularity *= BASE_DECAY + 0.1 * Math.exp(-rankingEngine.getRank(info.dataId) / 50.0);
        }
    }

    // 活跃状态处理（论文4.1节）
    private void updateActiveState(DataGenerator.DataInfo info, long currentCycle) {
        // 论文中的突发性增长模式
        if (shouldApplySurge(info, currentCycle)) {
            applyPopularitySurge(info);
        }

        info.activeDuration++;
        info.lastActiveTime = currentCycle;
    }

    // 不活跃状态处理（论文4.3节）
    private void updateInactiveState(DataGenerator.DataInfo info, long currentCycle) {
        info.inactiveDuration++;

        // 论文中的重新激活机制
        if (shouldReactivate(info)) {
            reactivateData(info, currentCycle);
        }
    }

    // 论文中的双模式增长判断（第5章）
    private boolean shouldApplySurge(DataGenerator.DataInfo info, long currentCycle) {
        return (random.nextDouble() < EXTERNAL_EVENT_PROB) ||
                (currentCycle - info.lastSurgeTime > 10);
    }

    // 论文式突发增长（公式5）
    private void applyPopularitySurge(DataGenerator.DataInfo info) {
        if (random.nextDouble() < EXTERNAL_EVENT_PROB) {
            // 外部事件驱动的突发（30%）
            info.popularity += 50 * Math.pow(random.nextDouble(), -0.8);
        } else {
            // 内部积累效应（70%）
            info.popularity *= 1.0 + 0.2 * Math.log(info.activeDuration + 1);
        }
        info.lastSurgeTime = CommonState.getTime();
    }

    // 论文式重新激活（图4）
    private boolean shouldReactivate(DataGenerator.DataInfo info) {
        double residenceTime = CommonState.getTime() - info.lastActiveTime;
        double threshold = 10 * Math.pow(info.peakPopularity, -0.6);
        return residenceTime > threshold &&
                random.nextDouble() < 0.1 * Math.log(info.historicalPopularity + 1);
    }

    private void reactivateData(DataGenerator.DataInfo info, long currentCycle) {
        info.popularity = 1 + Math.pow(random.nextDouble(), -1 / (POWER_LAW_EXPONENT - 1));
        info.activeDuration = 0;
        info.lastActiveTime = currentCycle;
    }

    // 论文启发的查询分配（第3章）
    private void distributeQueries() {
        List<DataGenerator.DataInfo> rankedItems = rankingEngine.getRankedList();

        for (int i = 0; i < TOTAL_QUERIES_PER_CYCLE; i++) {
            DataGenerator.DataInfo selected = selectByRank(rankedItems);
            executeQuery(selected.dataId);
        }
    }

    // 论文式排名加权选择（公式2）
    private DataGenerator.DataInfo selectByRank(List<DataGenerator.DataInfo> items) {
        double rankWeight = Math.pow(random.nextDouble(), -1 / POWER_LAW_EXPONENT);
        int index = (int) (rankWeight % items.size());
        return items.get(index);
    }

    private void executeQuery(BigInteger dataId) {
        DataGenerator.DataInfo info = dataRegistry.get(dataId);
        if (info == null) return;

        Node start = getRandomUpNode();
        if (start == null) return;

        // 论文中的查询反馈效应
        info.popularity += 1 + 0.1 * Math.log(rankingEngine.getRank(dataId) + 1);

        VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);
        Transport transport = (Transport) start.getProtocol(FastConfig.getTransport(pid));
        transport.send(start, getRandomUpNode(),
                new VLookupMessage(dataId, p.nodeId, true, 0, 0, CommonState.getTime()), pid);
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

    // 论文启发的排名引擎（第3章）
    class RankingEngine {
        private Map<BigInteger, Integer> currentRanks = new HashMap<>();
        private List<DataGenerator.DataInfo> rankedItems = new ArrayList<>();

        public void updateRanks(List<DataGenerator.DataInfo> activeItems) {
            // 论文式排名算法
            activeItems.sort((a, b) -> Double.compare(b.popularity, a.popularity));

            rankedItems = activeItems;
            currentRanks.clear();
            for (int i = 0; i < activeItems.size(); i++) {
                currentRanks.put(activeItems.get(i).dataId, i + 1);
            }
        }

        public int getRank(BigInteger dataId) {
            return currentRanks.getOrDefault(dataId, Integer.MAX_VALUE);
        }

        public List<DataGenerator.DataInfo> getRankedList() {
            return Collections.unmodifiableList(rankedItems);
        }
    }
}