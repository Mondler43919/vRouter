package vRouter;

/**
 * A Kademlia implementation for PeerSim extending the EDProtocol class.<br>
 * See the Kademlia bibliografy for more information about the protocol.
 *
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

import kademlia.*;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import redis.clients.jedis.Jedis;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class VRouterProtocol implements Cloneable, EDProtocol,CDProtocol {

	// VARIABLE PARAMETERS
	final String PAR_K = "K";  // 每个节点路由表中存储的最大邻居节点数
	final String PAR_ALPHA = "ALPHA";  // 每次并行查询的节点数
	final String PAR_BITS = "BITS";  // 节点ID的位数
	final String PAR_EXPECTED_ELEMENTS = "EXPECTED_ELEMENTS";  // 布隆过滤器预计存储的元素数量
	final String FALSE_POSITIVE_PROB = "FALSE_POSITIVE_PROB";  // 布隆过滤器的假阳性概率

	private static String prefix = null;  // 配置文件前缀，用于加载配置
	private int vRouterID;  // 当前协议的ID

	// 定义了多个队列，用于存储不同类型的消息
	public Queue<VLookupMessage> lookupMessages;  // 查找消息队列
	public Queue<IndexMessage> indexMessages;  // 索引消息队列
	public HashMap<BigInteger,Integer> dataStorage;  // 数据存储，存储数据ID与数据
	public HashMap<BigInteger,Integer> dataResponse;  // 数据缓存，存储数据ID与数据

	public HashMap<BigInteger,Integer> handledIndex = new HashMap<>();  // 记录已处理的索引消息
	public HashMap<BigInteger,Integer> handledQuery = new HashMap<>();  // 记录已处理的查询消息
	public CacheEvictionManager cacheEvictionManager;  // 缓存淘汰管理器
	private TTLCacheManager ttlCacheManager;  // TTL 缓存管理器
	private Integer accessCount;
	private HashSet<String> uniqueAccessNodes;
	private HashMap<String, Integer> dataAccessCount;
	private HashMap<String, Set<String>> dataAccessNode;
	private List<AccessRecord> accessRecords = new ArrayList<>();
	private final AtomicLong lastCycle = new AtomicLong(-1);
	/**
	 * allow to call the service initializer only once
	 *
	 * 只允许调用一次服务初始化器
	 */
	private static boolean _ALREADY_INSTALLED = false;  // 用于确保初始化只执行一次

	/**
	 * nodeId of this pastry node
	 *
	 * 当前节点的 ID
	 */
	private Integer cycle;
	public BigInteger nodeId;  // 当前节点的ID

	/**
	 * routing table of this pastry node
	 *
	 * 当前节点的路由表
	 */
	public RoutingTable routingTable;  // 路由表，存储节点的路由信息

	/**
	 * routing table with BloomFilter
	 *
	 * 带有布隆过滤器的路由表，用于加速查询
	 */
	public BloomFilterRoutingTable bfRoutingTable;  // 带有布隆过滤器的路由表，用于加速查询

	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 *
	 * @return Object
	 */
	public Object clone() {
		VRouterProtocol dolly = new VRouterProtocol(VRouterProtocol.prefix);  // 克隆协议
		return dolly;  // 返回新的协议对象
	}

	/**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 *
	 * @param prefix
	 *            String
	 */
	public VRouterProtocol(String prefix) {
		this.nodeId = null; // empty nodeId  // 初始化时节点ID为空
		VRouterProtocol.prefix = prefix;  // 设置配置前缀
		_init();  // 调用初始化方法
		routingTable = new RoutingTable();  // 创建路由表对象
		bfRoutingTable = new BloomFilterRoutingTable();  // 创建布隆过滤器路由表对象
		dataStorage = new HashMap<>();  // 创建数据存储对象
		lookupMessages = new LinkedList<>();  // 创建查找消息队列
		indexMessages = new LinkedList<>();  //
		this.cacheEvictionManager = null;
		this.ttlCacheManager = null;
		cycle = Configuration.getInt("CYCLE");

		accessCount=0;
		uniqueAccessNodes=new HashSet<>();
		dataAccessCount=new HashMap<>();
		dataAccessNode=new HashMap<>();
	}

	/**
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 *
	 * 该过程仅调用一次，用于初始化 Kademlia 协议的内部状态。
	 * 每个节点共享相同的配置，因此只需调用一次该方法。
	 */
	private void _init() {
		// execute once
		if (_ALREADY_INSTALLED)  // 如果已经初始化过，则返回
			return;

		// read paramaters  // 从配置文件中读取参数
		KademliaCommonConfig.K = Configuration.getInt(prefix + "." + PAR_K, KademliaCommonConfig.K);  // 获取 K 值
		KademliaCommonConfig.ALPHA = Configuration.getInt(prefix + "." + PAR_ALPHA, KademliaCommonConfig.ALPHA);  // 获取 ALPHA 值
		KademliaCommonConfig.BITS = Configuration.getInt(prefix + "." + PAR_BITS, KademliaCommonConfig.BITS);  // 获取 BITS 值
		VRouterCommonConfig.EXPECTED_ELEMENTS = Configuration.getInt(prefix + "." + PAR_EXPECTED_ELEMENTS, VRouterCommonConfig.EXPECTED_ELEMENTS);  // 获取布隆过滤器预计存储元素数量
		VRouterCommonConfig.FALSE_POSITIVE_PROB = Configuration.getDouble(prefix + "." + FALSE_POSITIVE_PROB, VRouterCommonConfig.FALSE_POSITIVE_PROB);  // 获取假阳性概率

		_ALREADY_INSTALLED = true;  // 标记已初始化
	}

	/**
	 * Search through the network the Node having a specific node Id, by performing binary search (we concern about the ordering
	 * of the network).
	 *
	 * 通过执行二分查找，在网络中搜索具有特定节点 ID 的节点（我们关注网络的排序）
	 *
	 * @param searchNodeId
	 *            BigInteger
	 * @return Node
	 */
	public Node nodeIdtoNode(BigInteger searchNodeId) {
		if (searchNodeId == null)  // 如果查询的节点 ID 为 null，返回 null
			return null;

		int inf = 0;
		int sup = Network.size() - 1;
		int m;

		while (inf <= sup) {  // 执行二分查找
			m = (inf + sup) / 2;  // 计算中间索引
			Node node = Network.get(m);

			VRouterProtocol protocol = (VRouterProtocol) node.getProtocol(vRouterID);
			BigInteger mId = protocol.nodeId;

			if (mId.equals(searchNodeId))  // 如果找到目标节点，返回该节点
				return Network.get(m);

			if (mId.compareTo(searchNodeId) < 0)  // 如果中间节点 ID 小于目标节点 ID，继续查找右半部分
				inf = m + 1;
			else  // 如果中间节点 ID 大于目标节点 ID，继续查找左半部分
				sup = m - 1;
		}

		// 如果网络未排序，使用传统的查找方法
		BigInteger mId;
		for (int i = Network.size() - 1; i >= 0; i--) {
			mId = ((VRouterProtocol) Network.get(i).getProtocol(vRouterID)).nodeId;  // 获取节点 ID
			if (mId.equals(searchNodeId))  // 如果找到目标节点，返回该节点
				return Network.get(i);
		}
		return null;  // 未找到目标节点，返回 null
	}
	private void updateDataMetrics(String sourceNodeId, String dataId) {

		// 更新总访问次数
		accessCount+=1;
		// 更新独立访问节点数
		uniqueAccessNodes.add(sourceNodeId);
		// 更新数据访问次数
		dataAccessCount.put(dataId, dataAccessCount.getOrDefault(dataId, 0) + 1);
		dataAccessNode.computeIfAbsent(dataId, k -> new HashSet<>()).add(sourceNodeId);
	}
	private void initDataMetrics()
	{
		accessCount=0;
		uniqueAccessNodes.clear();
		dataAccessCount.clear();
		dataAccessNode.clear();
	}
	/**
	 * set the current NodeId
	 *
	 * 设置当前节点的 NodeId
	 *
	 * @param tmp
	 *            BigInteger
	 */
	public void setNodeId(BigInteger tmp) {
		this.nodeId = tmp;  // 设置当前节点的 ID
		this.routingTable.nodeId = tmp;  // 设置路由表中的节点 ID
		this.cacheEvictionManager = new CacheEvictionManager(
				"127.0.0.1",
				6379,
				this.nodeId.toString(),
				10,
				0.5, 0.2, 0.3
		);
		this.ttlCacheManager = new TTLCacheManager(
				"127.0.0.1",
				6379,
				this.nodeId.toString()
		);
	}

	@Override
	public void processEvent(Node node, int protocolID, Object event) {
		//System.out.println("当前协议ID: " + protocolID);
		this.vRouterID = protocolID;
		if (event instanceof VLookupMessage) {
			handleLookupMessage((VLookupMessage) event, protocolID);
		} else if (event instanceof IndexMessage) {
			handleIndexMessage((IndexMessage) event, protocolID);
		} else if (event instanceof DataAccessMessage) {
			CentralNodeManager manager = ((MyNode) node).getCentralNodeManager();
			manager.processDataAccessMessage((DataAccessMessage) event);
		} else if (event instanceof DataResponseMessage) {
			handleDataResponseMessage((DataResponseMessage) event);
		}
	}

//
//更新查询次数。
//如果数据已经找到，则跳过消息处理。
//如果数据本地没有，则尝试通过反向路由表进行查找。
//如果找到数据，则停止查询。
//如果没有找到数据，则继续将查找消息转发到更接近的数据节点。
	public void handleLookupMessage(VLookupMessage msg, int protocolID) {
		//	打印日志，同时记录日志
		long currentCycle = peersim.core.CommonState.getTime()/cycle;
		long timestamp = System.currentTimeMillis();
		String input = timestamp + "|" + this.nodeId + "|" + msg.from + "|" + msg.dataID;
		String hash = HashUtils.SHA256(input);

		ExcelLogger.logDataAccess(currentCycle, timestamp,this.nodeId, msg.from, msg.dataID,hash);
		AccessRecord record = new AccessRecord(timestamp, this.nodeId, msg.from, msg.dataID,hash);
		accessRecords.add(record);

		updateDataMetrics(msg.from.toString(),msg.dataID.shiftRight(72).toString());

		if(VRouterObserver.dataQueryTraffic.get(msg.dataID) != null) {  // 如果数据查询已经有记录，更新查询次数
			int msgs = VRouterObserver.dataQueryTraffic.get(msg.dataID);
			msgs++;
			VRouterObserver.dataQueryTraffic.put(msg.dataID, msgs);
		}
		if(handledQuery.containsKey(msg.dataID)) return;
		BigInteger key = msg.dataID;
		// 先查 Redis
		String cachedData = cacheEvictionManager.get(key.toString());
		if (cachedData != null) {
			System.out.println("缓存命中：" + key + " -> " + cachedData);
			QueryGenerator.queriedData.put(msg.dataID, 1);  // 标记数据已查询
			VRouterObserver.successLookupForwardHop.add(msg.forwardHops);  // 记录向前跳数
			VRouterObserver.successLookupBackwardHop.add(msg.backwardHops);  // 记录向后跳数
			VRouterObserver.totalSuccessHops.add(msg.forwardHops + msg.backwardHops);  // 记录总跳数
			DataResponseMessage responseMsg = new DataResponseMessage(msg.dataID,Integer.parseInt(cachedData), this.nodeId, msg.initialRequester,0,msg.forwardHops+ msg.backwardHops,0, msg.initialTime);
			// 发送数据
			Node requesterNode = nodeIdtoNode(msg.initialRequester);
			VRouterProtocol requesterProtocol = (VRouterProtocol) requesterNode.getProtocol(protocolID);
			requesterProtocol.handleDataResponseMessage(responseMsg); // 直接调用处理函数

			return;
		}
		// 如果本地存储了数据
		if(dataStorage.containsKey(msg.dataID)) {
			QueryGenerator.queriedData.put(msg.dataID, 1);  // 标记数据已查询
			VRouterObserver.successLookupForwardHop.add(msg.forwardHops);  // 记录向前跳数
			VRouterObserver.successLookupBackwardHop.add(msg.backwardHops);  // 记录向后跳数
			VRouterObserver.totalSuccessHops.add(msg.forwardHops + msg.backwardHops);  // 记录总跳数
			DataResponseMessage responseMsg = new DataResponseMessage(msg.dataID,dataStorage.get(msg.dataID), this.nodeId, msg.initialRequester,0,msg.forwardHops+ msg.backwardHops,0,msg.initialTime);
			Node requesterNode = nodeIdtoNode(msg.initialRequester);
			VRouterProtocol requesterProtocol = (VRouterProtocol) requesterNode.getProtocol(protocolID);
			requesterProtocol.handleDataResponseMessage(responseMsg); // 直接调用处理函数
			return;
		}

		// 如果数据不在本地，查找反向路由表
		List<BigInteger> backwardList = bfRoutingTable.getMatch(msg.dataID);
		if(backwardList != null) {
			backwardList.removeIf(n -> Util.distance(n, msg.dataID).compareTo(Util.distance(this.nodeId, msg.dataID)) < 0);
			if(backwardList.size() > 0) {
				for (BigInteger n : backwardList) {
					Node nextHop = this.nodeIdtoNode(n);  // 获取下一跳节点
					if (nextHop != null) {
						VLookupMessage nextMsg = msg.backward(this.nodeId);
						sendMessage(nodeIdtoNode(nodeId),nextHop, protocolID, nextMsg);
					}
				}
			}
		}

		// 向前传递消息
		if(msg.direction) {
			VLookupMessage nextHop = msg.forward(this.nodeId);  // 创建向前的查找消息
			BigInteger[] closerNodes = getCloserNodes(nextHop.dataID);  // 获取更接近目标数据 ID 的节点
			for(int i = 0; i < closerNodes.length; i++) {
				Node targetNode = this.nodeIdtoNode(closerNodes[i]);  // 获取目标节点
//				VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);
//				targetPro.lookupMessages.add(nextHop);  // 将消息添加到目标节点的查找消息队列
				sendMessage(nodeIdtoNode(nodeId),targetNode, protocolID, nextHop);

			}
		}
		handledQuery.put(msg.dataID, 1);  // 标记该查询消息已处理
	}

	//此函数处理索引消息：
	//更新布隆过滤器。
	//如果该数据的索引消息尚未处理，则继续向前传递该索引消息，直到数据到达最接近的节点
	public void handleIndexMessage(IndexMessage msg, int protocolID) {
		//System.out.println("index");
		this.vRouterID = protocolID;
		if(VRouterObserver.dataIndexTraffic.get(msg.dataID) != null) {  // 如果索引消息已有记录，更新次数
			int msgs = VRouterObserver.dataIndexTraffic.get(msg.dataID);
			msgs++;
			VRouterObserver.dataIndexTraffic.put(msg.dataID, msgs);
		}

		// 获取并更新布隆过滤器
		ContactWithBloomFilter bfContact = this.bfRoutingTable.get(msg.from);
		if(bfContact == null) {
			bfContact = new ContactWithBloomFilter(msg.from);  // 如果没有找到该节点，创建新的布隆过滤器
			this.bfRoutingTable.put(bfContact);
		}
		bfContact.add(msg.dataID);  // 将数据 ID 添加到布隆过滤器中

		// 如果该索引消息已处理，跳过
		if(handledIndex.containsKey(msg.dataID)) {
			return;
		}

		IndexMessage relay = msg.relay(this.nodeId);  // 创建新的中继消息
		BigInteger[] closerNodes = getCloserNodes(msg.dataID);  // 获取更接近目标数据的节点

		// 将中继消息发送给更接近的数据节点
		for (BigInteger closerNode : closerNodes) {
			Node targetNode = this.nodeIdtoNode(closerNode);
//			VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);
//			targetPro.indexMessages.add(relay);  // 将消息添加到目标节点的索引消息队列
			sendMessage(nodeIdtoNode(nodeId),targetNode, protocolID, relay);

		}
		// 如果没有更接近的节点，记录当前节点的跳数
		if(closerNodes.length == 0) {
			VRouterObserver.indexHop.add(msg.hops);  // 记录索引消息的跳数
		}
		handledIndex.put(msg.dataID, 1);  // 标记该索引消息已处理
	}
	//该函数用于存储数据并生成相应的索引消息，之后将消息发送到更接近数据的节点。
	public void storeData(BigInteger dataID, int protocolID) {
		dataStorage.put(dataID, 0);  // 将数据存储到本地
		IndexMessage msg = new IndexMessage(dataID, this.nodeId);  // 创建索引消息
		BigInteger[] closerNodes = getCloserNodes(msg.dataID);  // 获取更接近目标数据的节点

		// 将索引消息发送给更接近的节点
		for (int i = 0; i < closerNodes.length; i++) {
			Node targetNode = this.nodeIdtoNode(closerNodes[i]);
			VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);
			targetPro.indexMessages.add(msg);  // 将索引消息添加到目标节点的队列中
			sendMessage(nodeIdtoNode(nodeId),targetNode, protocolID, msg);

		}
	}
	//该方法获取目标数据的邻居节点，并返回与目标数据更接近的节点
	public BigInteger[] getCloserNodes(BigInteger targetID) {
		BigInteger[] neighbors = routingTable.getNeighbours(targetID);  // 获取目标数据的邻居节点

		int targetFlag = 0;
		for (int i = 0; i < neighbors.length; i++) {
			// 如果目标节点更远，停止
			if (Util.distance(neighbors[i], targetID).compareTo(Util.distance(this.nodeId, targetID)) >= 0) break;
			targetFlag++;
		}

		BigInteger[] closerNodes = new BigInteger[Math.min(targetFlag, KademliaCommonConfig.ALPHA)];
		// 复制更接近的邻居节点
		for (int i = 0; i < Math.min(targetFlag, KademliaCommonConfig.ALPHA); i++) {
			closerNodes[i] = neighbors[i];
		}
		return closerNodes;  // 返回更接近的节点
	}
	public void handleDataResponseMessage(DataResponseMessage msg) {

		long currentTime = CommonState.getTime();
		;
		long latency = currentTime - msg.initialTime;
		//System.out.println("msg"+msg.initialTime+";当前时间"+currentTime+"latency"+latency);
		String dataKey = msg.dataID.toString();

		// 先检查数据是否存在于缓存
		String cachedData = cacheEvictionManager.get(dataKey);
		if (cachedData != null) { // 缓存存在时才更新频率
			double existingFrequency = cacheEvictionManager.getFrequency(dataKey);
			if (existingFrequency != -1.0) { // 确保有效值
				double newFrequency = existingFrequency + 1;
				cacheEvictionManager.addToCache(dataKey, newFrequency, msg.distance, msg.activityScore, msg.data);
				//ttlCacheManager.setTTL(msg.dataID.toString(), 60);
			} else { // 频率字段丢失时重新初始化
				cacheEvictionManager.addToCache(dataKey, 1.0, msg.distance, msg.activityScore, msg.data);
				//ttlCacheManager.setTTL(msg.dataID.toString(), 60);
			}
		} else { // 缓存不存在时直接添加
			cacheEvictionManager.addToCache(dataKey, 1.0, msg.distance, msg.activityScore, msg.data);
			//ttlCacheManager.setTTL(msg.dataID.toString(), 60);
		}
		VRouterObserver.latencyStats.add(latency);

	}
	public List<AccessRecord> getAccessRecords() {
		return new ArrayList<>(accessRecords);
	}

	public void clearAccessRecords() {
		accessRecords.clear();
	}


	public void sendMessage(Node sender, Node targetNode, int protocolID, Object message) {
		int transportPid = FastConfig.getTransport(protocolID);
		Transport transport = (Transport) sender.getProtocol(transportPid);
		transport.send(sender, targetNode, message, protocolID);
	}
	@Override
	public void nextCycle(Node node, int protocolID) {
		long time=CommonState.getTime();
		long currentCycle = time / cycle;
		if( QueryGenerator.executeFlag && lastCycle.getAndSet(currentCycle) < currentCycle){
			MyNode myNode = (MyNode) node;
			this.vRouterID = protocolID;
			BigInteger centralNodeID = new BigInteger(myNode.getBlockchain().getCentralNodeId());
			handleRegularNodeTasks(node, centralNodeID, currentCycle);
			// System.out.println("时间 " +time+"   from:"+node.getID()+"   to:"+centralNodeID);
		}
	}
	private void handleRegularNodeTasks(Node node,BigInteger centralNodeID, long currentCycle) {

		BigInteger input=BigInteger.valueOf(Instant.now().toEpochMilli());
		VRFElection.VRFOutput vrfoutput=((MyNode)node).generateVRFOutput(input);

		DataAccessMessage message =new DataAccessMessage(
				this.nodeId,
				NodeActivityScore.calculateActivityScore(accessCount, uniqueAccessNodes.size(), dataAccessCount),
				accessCount,
				uniqueAccessNodes.size(),
				new HashMap<>(dataAccessCount),
				new HashMap<>(dataAccessNode),
				new ArrayList<>(accessRecords),
				input,
				vrfoutput,
				currentCycle
		);
		sendMessage(node, nodeIdtoNode(centralNodeID), vRouterID, message);
		initDataMetrics();
	}
}