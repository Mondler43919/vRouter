package kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;

/**
 * This control generates random search traffic from nodes to random destination node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

// ______________________________________________________________________________________________
public class TrafficGenerator implements Control {

	// ______________________________________________________________________________________________
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_PROT = "protocol";

	/**
	 * MSPastry Protocol ID to act
	 */
	private final int pid;
	UniformRandomGenerator urg;

	// ______________________________________________________________________________________________
	public TrafficGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
	}

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a random find node message
	 * 
	 * @return boolean
	 */
	public boolean execute() {
		Node start;
		do {
			start = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((start == null) || (!start.isUp()));

		KademliaProtocol p = (KademliaProtocol)start.getProtocol(pid);

		BigInteger target = urg.generate();
		while(KademliaObserver.searchTraffic.containsKey(target)){
			target = urg.generate();
		}
		KademliaObserver.searchTraffic.put(target,0);
		LookupMessage msg = new LookupMessage(target,p.nodeId);
		KademliaObserver.msg_deliv.add(1);
		p.sendMessage(msg);
		return false;
	}

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
