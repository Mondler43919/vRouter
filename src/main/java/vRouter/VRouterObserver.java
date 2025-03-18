package vRouter;
//缓存修改
import com.google.gson.Gson;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the network
 * 统计和观察网络中的搜索时间、跳数等指标。
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class VRouterObserver implements Control {

	/**
	 * keep statistics of the number of hops of every successed lookup message.
	 *统计成功的向前跳数
	 */

	public static IncrementalStats successLookupForwardHop = new IncrementalStats();

	/**
	 * keep statistics of the number of hops of every successed lookup message.
	 */
	public static IncrementalStats successLookupBackwardHop = new IncrementalStats();

	/**
	 * keep statistics of the number of hops of every lookup message.
	 */
//	public static IncrementalStats droppedLookupMessage = new IncrementalStats();

//	public static IncrementalStats totalQuery = new IncrementalStats();

	/**
	 * keep statistic of number of hops of failed lookup message.
	 * 统计成功的查找总跳数
	 */
	public static IncrementalStats totalSuccessHops = new IncrementalStats();

	/**
	 * keep statistic of number of hops of failed lookup message.
	 */
	public static IncrementalStats failedBackwardLookupHop = new IncrementalStats();

	/**
	 * keep statistic of number of hop of index message.
	 *  统计索引消息的跳数
	 */
	public static IncrementalStats indexHop = new IncrementalStats();

	/**
	 * keep statistic of number of hop of index message.
	 *  统计布隆过滤器的数量
	 */
	public static IncrementalStats bloomFilterCount = new IncrementalStats();
	public static IncrementalStats latencyStats = new IncrementalStats();
	public static HashMap<BigInteger,Integer> dataIndexTraffic = new HashMap<>();

	public static HashMap<BigInteger,Integer> dataQueryTraffic = new HashMap<>();

	/** Parameter of the protocol we want to observe */
	private static final String PAR_PROT = "protocol";

	/** Protocol id */
	private int pid;

	/** Prefix to be printed in output */
	private String prefix;

	private int round = 1;

	public VRouterObserver(String prefix) {
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
		//存储效率评估
		String storeS = String.format("[time=%d]:[N=%d current nodes UP] [D=%f max index h] [%f avg index h] [%d total bf n] [%f avg bf perNode]",
				CommonState.getTime(),
				sz,
				indexHop.getMax(),
				indexHop.getAverage(),
				bloomFilterCount.getN(),
				((float) bloomFilterCount.getN() / (float) Network.size())
		);

		String storeC = String.format("%f",
				((float) bloomFilterCount.getN() / (float) Network.size())
		);

		String indexC = String.format("%f",
				((float) bloomFilterCount.getN() / (float) Network.size())
		);



//		System.err.println(storeS);
//		System.err.println(queryS);

//		System.err.println("=============================================");

//		if(CommonState.getTime() == 99){
//			for (BigInteger key: QueryGenerator.queriedData.keySet()
//			) {
//				if(QueryGenerator.queriedData.get(key)== 0)
//					System.err.println("missed data: " + key);
//			}
//			System.err.println("Debug target: " + QueryGenerator.DEBUGTARGET);
//			System.err.println("Index Path: " + new Gson().toJson(QueryGenerator.indexPath));
//			System.err.println("Index to Path: " + new Gson().toJson(QueryGenerator.indexToPath));
//			System.err.println("Query Path: " + new Gson().toJson(QueryGenerator.queryPath));
//		}

		/*索引跳数*/
//		String chartS = String.format("%f %f",
//				indexHop.getMax(),
//				indexHop.getAverage()
//		);

//		/*节点平均BF数量*/
//		String chartS = String.format("%f",
//				((float) bloomFilterCount.getN() / (float) Network.size())
//		);

		//查询效率评估
		String queryS = String.format("%f,%f",
				totalSuccessHops.getMax(),
				totalSuccessHops.getAverage()
//				successLookupForwardHop.getMax(),
//				successLookupForwardHop.getAverage(),
//				successLookupBackwardHop.getMax(),
//				successLookupBackwardHop.getAverage()
		);


		System.err.println(queryS);
		String latencyS = String.format("Latency: max=%f ms, avg=%f ms",
				latencyStats.getMax(),
				latencyStats.getAverage()
		);


		return false;
	}
}
