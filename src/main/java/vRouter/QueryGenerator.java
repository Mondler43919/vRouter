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
    public static boolean executeFlag = false;
    public static HashMap<BigInteger, Integer> queriedData = new HashMap<>();
    public static List<BigInteger> availableDataList;

    // 硬编码配置参数
    private final static String QUERY_MODE = "RANDOM";      // FIXED/LIST/RANDOM
    private final static int QUERY_FIXED_NODE = 5;        // FIXED模式使用的节点ID
    private final static List<Integer> QUERY_START_NODES = Arrays.asList(2, 7, 9); // LIST模式使用的节点列表

    private final int pid;
    private UniformRandomGenerator urg;

    public QueryGenerator(String prefix) {
        pid = Configuration.getPid(prefix + ".protocol");
        availableDataList = new ArrayList<>();
        urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);

        System.out.println("[Hardcoded Config] QUERY_MODE=" + QUERY_MODE
                + ", FIXED_NODE=" + QUERY_FIXED_NODE
                + ", START_NODES=" + QUERY_START_NODES);
    }

    public boolean execute() {
        if (!executeFlag || availableDataList.isEmpty()) return false;

       BigInteger query = availableDataList.get(CommonState.r.nextInt(availableDataList.size()));
       // BigInteger query = availableDataList.get(0);

        //VRouterObserver.dataQueryTraffic.put(query, 0);

        // 根据模式选择起始节点
        Node start = selectStartNode();
        if (start == null) {
            System.err.println("无法选择有效起始节点，跳过本次查询");
            return false;
        }

        // 随机选择目标节点
        Node targetNode = getRandomUpNode();
        if (targetNode == null) {
            System.err.println("无法选择有效目标节点，跳过本次查询");
            return false;
        }

        // 发送查询消息
        VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);
        long initialTime = CommonState.getTime();
        VLookupMessage lookupMessage = new VLookupMessage(query, p.nodeId, true, 0, 0, initialTime);
        sendMessage(start, targetNode, pid, lookupMessage);

        return false;
    }

    private Node selectStartNode() {
        switch (QUERY_MODE.toUpperCase()) {
            case "FIXED":
                Node node = Network.get(QUERY_FIXED_NODE);
                if (node == null || !node.isUp()) {
                    return getRandomUpNode();
                }
                return node;
            case "LIST":
                if (!QUERY_START_NODES.isEmpty()) {
                    int nodeId = QUERY_START_NODES.get(CommonState.r.nextInt(QUERY_START_NODES.size()));
                    Node listNode = Network.get(nodeId);
                    if (listNode != null && listNode.isUp()) {
                        return listNode;
                    }
                }
                return getRandomUpNode();
            case "RANDOM":
            default:
                return getRandomUpNode();
        }
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