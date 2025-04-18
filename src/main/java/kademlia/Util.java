package kademlia;

import java.math.BigInteger;

/**
 * Some utility and mathematical function to work with BigInteger numbers and strings.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class Util {

	/**
	 * Given two numbers, returns the length of the common prefix, i.e. how many digits (in base 2) have in common from the
	 * leftmost side of the number
	 * 
	 * @param b1
	 *            BigInteger
	 * @param b2
	 *            BigInteger
	 * @return int
	 */
	public static final int prefixLen(BigInteger b1, BigInteger b2) {

		String s1 = kademlia.Util.put0(b1);
		String s2 = kademlia.Util.put0(b2);

		int i = 0;
		for (i = 0; i < s1.length(); i++) {
			if (s1.charAt(i) != s2.charAt(i))
				return i;
		}

		return i;
	}

	/**
	 * return the distance between two number wich is defined as (a XOR b)
	 * 
	 * @param a
	 *            BigInteger
	 * @param b
	 *            BigInteger
	 * @return BigInteger
	 */
	public static final BigInteger distance(BigInteger a, BigInteger b) {
		return a.xor(b);
	}

	/**
	 * convert a BigInteger into a String (base 2) and lead all needed non-significative zeroes in order to reach the canonical
	 * length of a nodeid
	 * 
	 * @param b
	 *            BigInteger
	 * @return String
	 */
	public static final String put0(BigInteger b) {
		if (b == null)
			return null;
		String s = b.toString(2); // base 2
		while (s.length() < KademliaCommonConfig.BITS) {
			s = "0" + s;
		}
		return s;
	}

}
