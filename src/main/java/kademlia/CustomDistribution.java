package kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;

import java.math.BigInteger;

/**
 * This control initializes the whole network (that was already created by peersim) assigning a unique NodeId, randomly generated,
 * to every node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class CustomDistribution implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";

	private int protocolID;
	private UniformRandomGenerator urg;

	public CustomDistribution(String prefix) {
		protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
	}

	/**
	 * Scan over the nodes in the network and assign a randomly generated NodeId in the space 0..2^BITS, where BITS is a parameter
	 * from the kademlia protocol (usually 160)
	 * 
	 * @return boolean always false
	 */
	public boolean execute() {
		BigInteger tmp;
		for (int i = 0; i < Network.size(); ++i) {
			tmp = urg.generate();
			if(tmp.equals(new BigInteger("1401212130835375394797907194136709459790834381274"))){
				System.err.println("node num: " + i);
			}
			((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setNodeId(tmp);
		}

		return false;
	}

}
