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
    // 数据分类配置

    public static boolean executeFlag = false;
    public static HashMap<BigInteger, DataGenerator.DataInfo> dataRegistry = new HashMap<>();
    public static HashMap<BigInteger, Integer> queriedData = new HashMap<>();

    // 配置参数（可通过配置文件覆盖）
    private final static String PAR_DATA_DISTRIBUTION = "SHORT_TERM_INACTIVE:0.4,LONG_TERM_ACTIVE:0.3,MALICIOUS_SPAM:0.1,LONG_TERM_INACTIVE:0.2";
    private final static int PAR_TOTAL_NODES = Network.size();

    private final int pid;
    private UniformRandomGenerator urg;
    private final Random random = CommonState.r;

    public QueryGenerator(String prefix) {
        pid = Configuration.getPid(prefix + ".protocol");
        urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, random);

        // 初始化数据分类
        initDataDistribution(prefix);
        System.out.println("[QueryGenerator] Initialized with " + dataRegistry.size() + " data items");
    }

    private void initDataDistribution(String prefix) {
        String[] distConfig = Configuration.getString(prefix + ".data_distribution", PAR_DATA_DISTRIBUTION).split(",");
        Map<DataGenerator.DataType, Double> probMap = new HashMap<>();
    }

    public boolean execute() {
        if (!executeFlag || dataRegistry.isEmpty())
            return false;

        // 选择数据
        BigInteger query = selectDataByPopularity();
        DataGenerator.DataInfo dataInfo = dataRegistry.get(query);

        // 选择节点
        Node start = selectStartNode(dataInfo);
        if (start == null) return false;

        // 发送查询
        VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);
        VLookupMessage msg = new VLookupMessage(
                query, p.nodeId, true, 0, 0, CommonState.getTime());
        sendMessage(start, selectTargetNode(dataInfo), pid, msg);

        return false;
    }

    private BigInteger selectDataByPopularity() {
        // 基于流行度的加权随机选择
        double totalPop = dataRegistry.values().stream()
                .mapToDouble(info -> info.popularity).sum();
        double rand = random.nextDouble() * totalPop;

        double cumulative = 0;
        for (BigInteger dataId : dataRegistry.keySet()) {
            cumulative += dataRegistry.get(dataId).popularity;
            if (rand <= cumulative) {
                return dataId;
            }
        }
        return dataRegistry.get(random.nextInt(dataRegistry.size())).dataId;
    }

    private void updateDataState(DataGenerator.DataInfo info, long currentTime) {
        switch (info.type) {
            case SHORT_TERM_INACTIVE:
                // U型流行度 + 节点变化
                double phase = (currentTime % 24) / 24.0; // 假设周期为24
                info.popularity = 50 + 40 * Math.sin(2 * Math.PI * (phase - 0.25));
                info.activeNodes = (int) (PAR_TOTAL_NODES * 0.3 * (1 + 0.5 * Math.sin(2 * Math.PI * (phase - 0.2))));
                break;

            case MALICIOUS_SPAM:
                // 突发性高流行度 + 极少节点
                if (random.nextDouble() < 0.05) { // 5%概率触发攻击
                    info.popularity = 500 + random.nextInt(300);
                    info.activeNodes = 1 + random.nextInt(2);
                } else {
                    info.popularity = 5 + random.nextInt(10);
                    info.activeNodes = 1;
                }
                break;

            case LONG_TERM_ACTIVE:
                // 稳定高流行度 + 多节点
                info.popularity = 80 + 20 * Math.sin(2 * Math.PI * currentTime / 24);
                info.activeNodes = (int) (PAR_TOTAL_NODES * (0.6 + 0.1 * random.nextDouble()));
                break;

            case LONG_TERM_INACTIVE:
                // 周期性衰减
                double lifePhase = (currentTime % 72) / 72.0; // 假设3周期
                info.popularity = 40 * (1 - lifePhase * 0.8);
                info.activeNodes = (int) (PAR_TOTAL_NODES * 0.4 * (1 - lifePhase * 0.5));
                break;
        }
    }

    private Node selectStartNode(DataGenerator.DataInfo info) {
        // 恶意流量固定从少数节点发起
        if (info.type == DataGenerator.DataType.MALICIOUS_SPAM) {
            return Network.get(info.activeNodes); // 直接使用活跃节点数作为ID
        }

        // 其他类型随机选择（可扩展为基于节点分布）
        return getRandomUpNode();
    }

    private Node selectTargetNode(DataGenerator.DataInfo info) {
        // 长期活跃数据更可能被随机节点请求
        if (info.type == DataGenerator.DataType.LONG_TERM_ACTIVE) {
            return getRandomUpNode();
        }
        // 其他类型倾向于固定节点（模拟局部性）
        return Network.get(random.nextInt(Math.max(1, PAR_TOTAL_NODES / 10)));
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
        System.err.println("无法找到有效节点");
        return null;
    }

    private void sendMessage(Node sender, Node receiver, int protocolId, Object message) {
        int transportPid = FastConfig.getTransport(protocolId);
        Transport transport = (Transport) sender.getProtocol(transportPid);
        transport.send(sender, receiver, message, protocolId);
    }
}