package kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the network
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KademliaObserver implements Control {

	/**
	 * keep statistics of the number of hops of every message delivered.
	 */
	public static IncrementalStats successHop = new IncrementalStats();

	/**
	 * keep statistics of the number of hops of every message delivered.
	 */
	public static IncrementalStats totalHop = new IncrementalStats();

	/**
	 * keep statistic of number of message delivered
	 */
	public static IncrementalStats msg_deliv = new IncrementalStats();

	public static HashMap<BigInteger,Integer> searchTraffic = new HashMap<>();

	/** Parameter of the protocol we want to observe */
	private static final String PAR_PROT = "protocol";

	/** Protocol id */
	private int pid;

	/** Prefix to be printed in output */
	private String prefix;

	public KademliaObserver(String prefix) {
		this.prefix = prefix;
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	/**
	 * print the statistical snapshot of the current situation
	 * 
	 * @return boolean always false
	 */
	public boolean execute() {
		// get the real network size
		int sz = Network.size();
		for (int i = 0; i < Network.size(); i++)
			if (!Network.get(i).isUp())
				sz--;

		String s = String.format("[time=%d]:[N=%d current nodes UP] [D=%f msg deliv] [%f sum msg] [%f average h] [%f max h]", CommonState.getTime(), sz, msg_deliv.getSum(), totalHop.getSum(), successHop.getAverage(), successHop.getMax());

//		if (CommonState.getTime() == 3600000) {
//			// create hop file
//			try {
//				File f = new File("D:/simulazioni/hopcountNEW.dat"); // " + sz + "
//				f.createNewFile();
//				BufferedWriter out = new BufferedWriter(new FileWriter(f, true));
//				out.write(String.valueOf(hopStore.getAverage()).replace(".", ",") + ";\n");
//				out.close();
//			} catch (IOException e) {
//			}
//			// create latency file
//			try {
//				File f = new File("D:/simulazioni/latencyNEW.dat");
//				f.createNewFile();
//				BufferedWriter out = new BufferedWriter(new FileWriter(f, true));
//				out.write(String.valueOf(timeStore.getAverage()).replace(".", ",") + ";\n");
//				out.close();
//			} catch (IOException e) {
//			}
//
//		}
//

		int max = 0,min = Integer.MAX_VALUE;
		float sum = 0;
		for (Integer i:searchTraffic.values()) {
			max = Math.max(max,i);
			min = Math.min(min,i);
			sum += i;
		}
		float avg = sum/searchTraffic.size();
		//网络开销评估
		String trafficS = String.format("%d,%d,%f",
				max,min,avg);

		String chartS = String.format(
				"-----query:max=%f,avg=%f;traffic:max=%d,avg=%f",
				successHop.getMax(),successHop.getAverage()
				,max,avg);

		System.err.println(chartS);

		return false;
	}
}
