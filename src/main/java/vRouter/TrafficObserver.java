package vRouter;

import peersim.core.Control;
import peersim.core.Network;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

public class TrafficObserver implements Control {
	private FileWriter fileWriter; // 文件写入对象

	public TrafficObserver(String prefix) {
		try {
			// 初始化文件写入器，设置为追加模式
			fileWriter = new FileWriter("D:\\output.txt", true);
		} catch (IOException e) {
			System.err.println("无法创建或写入文件: " + e.getMessage());
		}
	}

	public boolean execute() {
		int sz = Network.size();
		for (int i = 0; i < Network.size(); i++) {
			if (!Network.get(i).isUp()) {
				sz--;
			}
		}

		// 构建输出字符串
		String indexS = String.format(
				"index hops:max=%f,avg=%f;",
				VRouterObserver.indexHop.getMax(),
				VRouterObserver.indexHop.getAverage()
		);
		String trafficI = calculateTraffic(VRouterObserver.dataIndexTraffic);
		String latencyS = String.format(
				"Latency: max=%f ms, avg=%f ms",
				VRouterObserver.latencyStats.getMax(),
				VRouterObserver.latencyStats.getAverage()
		);
		String output = indexS + trafficI + latencyS + "\n"; // 添加换行符
		System.err.println(indexS + trafficI+latencyS );
		// 写入文件
		if (fileWriter != null) {
			try {
				fileWriter.write(output);
				fileWriter.flush(); // 确保数据立即写入
			} catch (IOException e) {
				System.err.println("写入文件失败: " + e.getMessage());
			}
		}

		return false;
	}

	public String calculateTraffic(HashMap<BigInteger, Integer> trafficMap) {
		int max = 0;
		float sum = 0;
		for (Integer i : trafficMap.values()) {
			max = Math.max(max, i);
			sum += i;
		}
		float avg = sum / trafficMap.size();
		return String.format("traffic:max=%d,avg=%f;", max, avg);
	}

	// 关闭文件写入器（可选）
	@Override
	protected void finalize() throws Throwable {
		try {
			if (fileWriter != null) {
				fileWriter.close();
			}
		} finally {
			super.finalize();
		}
	}
}