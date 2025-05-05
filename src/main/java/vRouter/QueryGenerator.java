package vRouter;

import kademlia.KademliaCommonConfig;
import kademlia.UniformRandomGenerator;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;

import java.math.BigInteger;
import java.util.*;

public class QueryGenerator implements Control {
    private static final int TOTAL_QUERIES_PER_CYCLE = 1000;
    private static final double MARKOV_STABILITY = 0.93;         // 保持原排名的概率
    private static final double NEAR_RANK_TRANSITION = 0.05;     // 轻微跳动的概率
    private static final double ZIPF_ALPHA = 0.6;   //zipf越大数据越极端
    public static boolean executeFlag = false;
    public static HashMap<BigInteger, DataGenerator.DataInfo> dataRegistry = new HashMap<>();
    public static Set<String> dataPrefixes = new HashSet<>();
    public static HashMap<BigInteger, Integer> queriedData = new HashMap<>();

    private final int pid;
    private final UniformRandomGenerator urg;
    private final Random random = CommonState.r;
    private static List<BigInteger> globalRanking = new ArrayList<>();
    private static Map<BigInteger, Integer> rankMap = new HashMap<>();
    private static double[] zipfProbabilities = null;


    public QueryGenerator(String prefix) {
        pid = Configuration.getPid(prefix + ".protocol");
        urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, random);
    }

    public boolean execute() {
        if (!executeFlag) return false;

        // 1. 马尔可夫排名转移
        applyMarkovRankTransitions();

        // 2. 生成查询
        generateZipfQueries();

        return false;
    }
    // 初始化全局排名（在所有数据生成后调用一次）
    public static void initializeGlobalRanking() {
        globalRanking.clear();
        globalRanking.addAll(dataRegistry.keySet());

        // 初始Zipf排名
        Collections.shuffle(globalRanking); // 先随机打乱
        rankMap.clear();
        for (int i = 0; i < globalRanking.size(); i++) {
            rankMap.put(globalRanking.get(i), i+1);
        }
        int size = globalRanking.size();
        zipfProbabilities = new double[size];
        double totalWeight = 0.0;

        for (int i = 0; i < size; i++) {
            zipfProbabilities[i] = 1.0 / Math.pow(i + 1, ZIPF_ALPHA);
            totalWeight += zipfProbabilities[i];
        }

        for (int i = 0; i < size; i++) {
            zipfProbabilities[i] /= totalWeight;
        }
    }
    private void applyMarkovRankTransitions() {
        // 创建副本以避免修改原始数组
        List<BigInteger> newRanking = new ArrayList<>(globalRanking);

        for (int i = 0; i < newRanking.size(); i++) {
            BigInteger dataId = newRanking.get(i);
            int currentRank = i + 1;  // 数组索引从0开始，排名从1开始

            // 计算新的期望排名
            int newRank = calculateNewRank(currentRank);

            // 确保新排名有效且与当前不同
            if (newRank != currentRank && newRank > 0 && newRank <= newRanking.size()) {
                // 直接交换两个位置的数据
                Collections.swap(newRanking, currentRank-1, newRank-1);
            }
        }

        // 更新全局排名和排名映射
        updateGlobalRanking(newRanking);
    }

    private void updateGlobalRanking(List<BigInteger> newRanking) {
        globalRanking = newRanking;
        rankMap.clear();
        for (int i = 0; i < globalRanking.size(); i++) {
            rankMap.put(globalRanking.get(i), i+1);
        }
    }

    private int calculateNewRank(int currentRank) {
        double rand = random.nextDouble();
        if (rand < MARKOV_STABILITY) {
            return currentRank;
        } else if (rand < MARKOV_STABILITY + NEAR_RANK_TRANSITION) {
            int shift = random.nextInt(2) + 1;
            return currentRank + (random.nextBoolean() ? shift : -shift);
        } else {
            int shift = random.nextInt(3) + 2;
            return currentRank + (random.nextBoolean() ? shift : -shift);
        }
    }

    private void generateZipfQueries() {
        // 使用别名方法高效抽样
        for (int i = 0; i < TOTAL_QUERIES_PER_CYCLE; i++) {
            int selectedRank = sampleZipf();
            BigInteger dataId = globalRanking.get(selectedRank-1);
            executeQuery(dataId);
        }
    }
    private int sampleZipf() {
        double rand = random.nextDouble();
        double cumulative = 0.0;

        for (int i = 0; i < zipfProbabilities.length; i++) {
            cumulative += zipfProbabilities[i];
            if (rand < cumulative) {
                return i + 1;
            }
        }
        return zipfProbabilities.length;
    }

    private void executeQuery(BigInteger dataId) {
        DataGenerator.DataInfo info = dataRegistry.get(dataId);
        if (info == null) return;

        Node start = getRandomUpNode();
        if (start == null) return;

        VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);
        Transport transport = (Transport) start.getProtocol(FastConfig.getTransport(pid));
        transport.send(start, getRandomUpNode(),
                new VLookupMessage(dataId, p.nodeId, true, 0, 0, CommonState.getTime()), pid);
    }

    private Node getRandomUpNode() {
        int attempts = 0;
        while (attempts < Network.size()) {
            Node node = Network.get(CommonState.r.nextInt(Network.size()));
            if (node != null && node.isUp()) {
                return node;
            }
            attempts++;
        }
        return null;
    }

}
