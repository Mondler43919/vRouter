package kademlia;

/**
 * A Kademlia implementation for PeerSim extending the EDProtocol class.<br>
 * See the Kademlia bibliografy for more information about the protocol.
 *
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import vRouter.VRouterObserver;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

//__________________________________________________________________________________________________
public class KademliaProtocol implements Cloneable, CDProtocol {

	// VARIABLE PARAMETERS
	final String PAR_K = "K";
	final String PAR_ALPHA = "ALPHA";
	final String PAR_BITS = "BITS";

	private static String prefix = null;
	private int kademliaid;

	private Queue<LookupMessage> messageQueue;

	public HashMap<BigInteger,Integer> handledQuery = new HashMap<>();

	/**
	 * allow to call the service initializer only once
	 */
	private static boolean _ALREADY_INSTALLED = false;

	/**
	 * nodeId of this pastry node
	 */
	public BigInteger nodeId;

	/**
	 * routing table of this pastry node
	 */
	public RoutingTable routingTable;


	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 * 
	 * @return Object
	 */
	public Object clone() {
		kademlia.KademliaProtocol dolly = new kademlia.KademliaProtocol(kademlia.KademliaProtocol.prefix);
		return dolly;
	}

	/**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 * 
	 * @param prefix
	 *            String
	 */
	public KademliaProtocol(String prefix) {
		this.nodeId = null; // empty nodeId
		kademlia.KademliaProtocol.prefix = prefix;
		_init();
		routingTable = new RoutingTable();
		messageQueue = new LinkedList<>();
	}

	/**
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 */
	private void _init() {
		// execute once
		if (_ALREADY_INSTALLED)
			return;

		// read paramaters
		KademliaCommonConfig.K = Configuration.getInt(prefix + "." + PAR_K, KademliaCommonConfig.K);
		KademliaCommonConfig.ALPHA = Configuration.getInt(prefix + "." + PAR_ALPHA, KademliaCommonConfig.ALPHA);
		KademliaCommonConfig.BITS = Configuration.getInt(prefix + "." + PAR_BITS, KademliaCommonConfig.BITS);

		_ALREADY_INSTALLED = true;
	}

	/**
	 * Search through the network the Node having a specific node Id, by performing binary serach (we concern about the ordering
	 * of the network).
	 * 
	 * @param searchNodeId
	 *            BigInteger
	 * @return Node
	 */
	private Node nodeIdtoNode(BigInteger searchNodeId) {
		if (searchNodeId == null)
			return null;

		int inf = 0;
		int sup = Network.size() - 1;
		int m;

		while (inf <= sup) {
			m = (inf + sup) / 2;

			BigInteger mId = ((kademlia.KademliaProtocol) Network.get(m).getProtocol(kademliaid)).nodeId;

			if (mId.equals(searchNodeId))
				return Network.get(m);

			if (mId.compareTo(searchNodeId) < 0)
				inf = m + 1;
			else
				sup = m - 1;
		}

		// perform a traditional search for more reliability (maybe the network is not ordered)
		BigInteger mId;
		for (int i = Network.size() - 1; i >= 0; i--) {
			mId = ((kademlia.KademliaProtocol) Network.get(i).getProtocol(kademliaid)).nodeId;
			if (mId.equals(searchNodeId))
				return Network.get(i);
		}

		return null;
	}

	/**
	 * set the current NodeId
	 * 
	 * @param tmp
	 *            BigInteger
	 */
	public void setNodeId(BigInteger tmp) {
		this.nodeId = tmp;
		this.routingTable.nodeId = tmp;
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		this.kademliaid = protocolID;
		while(!messageQueue.isEmpty()){
			KademliaObserver.totalHop.add(1);
			LookupMessage msg = messageQueue.poll();
						assert msg != null;
			if(handledQuery.containsKey(msg.target)){
				return;
			}
			if(KademliaObserver.searchTraffic.get(msg.target)!=null){
				int msgs = KademliaObserver.searchTraffic.get(msg.target);
				msgs ++;
				KademliaObserver.searchTraffic.put(msg.target,msgs);
			}
			routingTable.addNeighbour(msg.from);
			BigInteger[] neighbors = getCloserNodes(msg.target);
			int targetFlag = 0;
			for(int i=0;i<neighbors.length;i++){
				//target is farther than local; stop
				if(Util.distance(neighbors[i],msg.target).compareTo(Util.distance(this.nodeId,msg.target)) >=0 ) break;
				targetFlag++;
			}

			//send message to closer node
			for(int i=0;i<targetFlag;i++){
				Node targetNode = this.nodeIdtoNode(neighbors[i]);
				KademliaProtocol targetPro = (KademliaProtocol) targetNode.getProtocol(protocolID);
				targetPro.sendMessage(msg.nextHop(this.nodeId));
			}

			//local node is the closest;
			if(targetFlag ==0){
				KademliaObserver.successHop.add(msg.hops);
			}
			handledQuery.put(msg.target,0);
		}
	}

	public BigInteger[] getCloserNodes(BigInteger targetID){
		BigInteger[] neighbors = routingTable.getNeighbours(targetID);

		int targetFlag = 0;
		for(int i=0;i<neighbors.length;i++){
			//target is farther than local; stop
			if(Util.distance(neighbors[i],targetID).compareTo(Util.distance(this.nodeId,targetID)) >=0 ) break;
			targetFlag++;
		}

		BigInteger[] closerNodes = new BigInteger[Math.min(targetFlag, KademliaCommonConfig.ALPHA)];
		//copy neighbours
		for(int i=0;i<Math.min(targetFlag, KademliaCommonConfig.ALPHA);i++){
			closerNodes[i] = neighbors[i];
		}
		return closerNodes;
	}

	public void sendMessage(LookupMessage target){
		messageQueue.add(target);
	}
}
