package vRouter;

import kademlia.KademliaCommonConfig;
import kademlia.UniformRandomGenerator;
import kademlia.Util;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.*;

/**
 * 初始化类，执行网络中所有初始节点的k-buckets填充操作。<br>
 * 具体来说，每个节点会被添加到网络中每个其他节点的路由表中。最终，尽管各个节点的路由表中会包含其他节点，
 * 但是因为k-bucket的容量有限，当k-bucket满时，会随机删除其中的一个节点。
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class StateBuilder implements peersim.core.Control {

	// 配置参数常量，表示协议名称
	private static final String PAR_PROT = "protocol";

	// 用于存储协议前缀
	private String prefix;

	// 存储虚拟路由器协议的ID
	private int vrouterID;

	// 构造方法，初始化协议前缀，并从配置文件中获取协议ID
	public StateBuilder(String prefix) {
		this.prefix = prefix;
		vrouterID = 0;
	}
	// 执行初始化操作
	public boolean execute() {
        System.out.println("开始网络初始化");
        // 创建一个均匀随机生成器，用于生成随机的节点ID
        UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
        int randomNodeId = CommonState.r.nextInt(Network.size());

        // 为每个节点生成一个随机的节点ID
        BigInteger centralNodeId = null;
        for (int i = 0; i < Network.size(); ++i) {
            BigInteger tmp;
            tmp = urg.generate();  // 生成一个随机的节点ID
            // 设置当前节点的虚拟路由器ID
            MyNode node = (MyNode) Network.get(i);
            node.setNodeId(tmp);
            ((VRouterProtocol) (node.getProtocol(vrouterID))).setNodeId(tmp);
            if (i == randomNodeId) {
                System.out.println("初始中心节点: " + tmp.bitLength());
                centralNodeId = tmp;
            }
        }
        BlockData genesisData = new BlockData(
                "0",
                new ArrayList<>(),      // 空 MerkleTree
                centralNodeId.toString(),               // 初始中心节点 ID
                new HashMap<>(),                        // 候选节点映射（空）
                new HashMap<>(),                        // 数据评分（空）
                new HashMap<>()                         // 节点评分（空）
        );
		Block genesisBlock = new Block("0", genesisData);
		for (int i = 0; i < Network.size(); i++) {
			MyNode node = (MyNode) Network.get(i);
			node.getBlockchain().forceSetGenesisBlock(genesisBlock); // 强制设置创世区块
		}
		System.out.println("创世区块已生成，初始中心节点: " + centralNodeId);

        // 按节点ID升序对网络中的所有节点进行排序
        Network.sort(new Comparator<Node>() {
            public int compare(Node o1, Node o2) {
                Node n1 = (Node) o1;
                Node n2 = (Node) o2;
                VRouterProtocol p1 = (VRouterProtocol) (n1.getProtocol(vrouterID));
                VRouterProtocol p2 = (VRouterProtocol) (n2.getProtocol(vrouterID));
                // 利用Util.put0方法对节点ID进行比较
                return Util.put0(p1.nodeId).compareTo(Util.put0(p2.nodeId));
            }
        });


        // 获取网络中的节点数量
        int sz = Network.size();

        // 遍历每个节点，为其路由表添加更多随机邻居
        for (int i = 0; i < sz; i++) {
            Node iNode = Network.get(i);
            VRouterProtocol iKad = (VRouterProtocol) (iNode.getProtocol(vrouterID));

            // 为当前节点添加100个随机邻居
            HashSet<BigInteger> added = new HashSet<>();
            while (added.size() < 100) {
                int randIndex = CommonState.r.nextInt(sz);
                if (randIndex == i) continue; // 不加自己
                VRouterProtocol jKad = (VRouterProtocol) (Network.get(randIndex).getProtocol(vrouterID));
                if (added.add(jKad.nodeId)) {
                    iKad.routingTable.addNeighbour(jKad.nodeId);
                }
            }
        }

        // 为每个节点添加80个“相对接近”的邻居节点
        for (int i = 0; i < sz; i++) {
            Node iNode = Network.get(i);
            VRouterProtocol iKad = (VRouterProtocol) (iNode.getProtocol(vrouterID));

            int start = Math.max(0, i - 40);  // 确保不越界，向前偏移40
            int count = 0;
            while (count < 100 && start < sz) {
                if (start != i) {
                    VRouterProtocol jKad = (VRouterProtocol) (Network.get(start).getProtocol(vrouterID));
                    iKad.routingTable.addNeighbour(jKad.nodeId);
                    count++;
                }
                start++;
            }
        }

        System.out.println("网络已初始化");
        return false;  // 返回false表示该控制器不再继续执行
    }
}