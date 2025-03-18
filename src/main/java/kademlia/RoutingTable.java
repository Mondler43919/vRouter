package kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Gives an implementation for the rounting table component of a kademlia node
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class RoutingTable implements Cloneable {

	// node ID of the node
	public BigInteger nodeId = null;

	// k-buckets
	public TreeMap<Integer, KBucket> k_buckets = null;

	// ______________________________________________________________________________________________
	/**
	 * instanciates a new empty routing table with the specified size
	 */
	public RoutingTable() {
		k_buckets = new TreeMap<Integer, KBucket>();
		// initialize k-bukets
		for (int i = 0; i <= KademliaCommonConfig.BITS; i++) {
			k_buckets.put(i, new KBucket());
		}
	}

	// add a neighbour to the correct k-bucket
	public void addNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		int prefix_len = Util.prefixLen(nodeId, node);

		// add the node to the k-bucket
		k_buckets.get(prefix_len).addNeighbour(node);
	}

	// remove a neighbour from the correct k-bucket
	public void removeNeighbour(BigInteger node) {
		// get the lenght of the longest common prefix (correspond to the correct k-bucket)
		int prefix_len = Util.prefixLen(nodeId, node);

		// add the node to the k-bucket
		k_buckets.get(prefix_len).removeNeighbour(node);
	}

	// return the closest neighbour to a key from the correct k-bucket
	public BigInteger[] getNeighbours(final BigInteger key) {
		// resulting neighbours
		BigInteger[] result = new BigInteger[KademliaCommonConfig.ALPHA];

		// neighbour candidates
		ArrayList<BigInteger> neighbour_candidates = new ArrayList<BigInteger>();

		// get the lenght of the longest common prefix
		int prefix_len = Util.prefixLen(nodeId, key);

		// return the k-bucket if is more than alpha
		if (k_buckets.get(prefix_len).neighbours.size() >= KademliaCommonConfig.ALPHA) {
			neighbour_candidates.addAll(k_buckets.get(prefix_len).neighbours.keySet());
		}
		else{
			// else get k closest node from all k-buckets
//			prefix_len = 0;
//			while (prefix_len < KademliaCommonConfig.ALPHA) {
//				neighbour_candidates.addAll(k_buckets.get(prefix_len).neighbours.keySet());
//				// remove source id
//				prefix_len++;
//			}
			while(neighbour_candidates.size()<KademliaCommonConfig.ALPHA && prefix_len > 0){
				prefix_len --;
				neighbour_candidates.addAll(k_buckets.get(prefix_len).neighbours.keySet());
			}
		}

		// create a map (distance, node), sort by distance
		TreeMap<BigInteger, BigInteger> distance_map = new TreeMap<BigInteger, BigInteger>();

		for (BigInteger node : neighbour_candidates) {
			distance_map.put(Util.distance(node, key), node);
		}

		int i = 0;
		for (BigInteger iii : distance_map.keySet()) {
			if (i < KademliaCommonConfig.ALPHA) {
				if(distance_map.get(iii) == null) continue;
				result[i] = distance_map.get(iii);
				i++;
			}
		}

		return result;
	}

	// ______________________________________________________________________________________________
	public Object clone() {
		kademlia.RoutingTable dolly = new kademlia.RoutingTable();
		for (int i = 0; i < KademliaCommonConfig.BITS; i++) {
			k_buckets.put(i, new KBucket());// (KBucket) k_buckets.get(i).clone());
		}
		return dolly;
	}

	// ______________________________________________________________________________________________
	/**
	 * print a string representation of the table
	 * 
	 * @return String
	 */
	public String toString() {
		return "";
	}
	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
