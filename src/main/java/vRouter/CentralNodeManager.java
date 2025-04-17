package vRouter;

import peersim.config.Configuration;
import peersim.core.Network;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class CentralNodeManager {
    private final MyNode currentNode;

    // 每轮缓存：key = round number
    private final Map<Long, RoundData> roundBuffer = new HashMap<>();

    public CentralNodeManager(MyNode currentNode) {
        this.currentNode = currentNode;
    }

    // 内部类：记录某一轮的所有数据
    private static class RoundData {
        int received = 0;
        final Map<String, DataAccessMessage> messages = new HashMap<>();
    }

    // === 接收每条节点消息（包含周期号）===
    public void processDataAccessMessage(DataAccessMessage message) {
        long round = message.cycle; // 消息携带的轮次
        String senderId = message.from.toString();

        // 拿到或创建该轮的 RoundData
        roundBuffer.putIfAbsent(round, new RoundData());
        RoundData roundData = roundBuffer.get(round);

        // 如果已收到该节点消息就跳过（防重）
        if (roundData.messages.containsKey(senderId)) return;

        // 存入消息
        roundData.messages.put(senderId, message);
        roundData.received++;
        // System.out.println(round + "    from:"+senderId+"   to:"+currentNode.getId());

        // 如果收到全网消息，触发该轮执行
        if (roundData.received == Network.size() - 1) { // -1 因为中心节点不发
            executeRound(round, roundData);
        }
    }

    // === 执行某一轮中心节点逻辑 ===
    private void executeRound(long round, RoundData roundData) {
        System.out.println("======= 执行第 " + round + " 轮 =======");

        // 初始化临时状态
        Map<String, Double> nodeScores = new HashMap<>();
        Map<String, Integer> globalAccessCounts = new HashMap<>();
        Map<String, Set<String>> globalAccessNodes = new HashMap<>();
        Map<String, NodeProof> nodeProofs = new HashMap<>();
        Map<String, BigInteger> nodeVrfOutputs = new HashMap<>();
        Map<String, BigInteger> nodeVrfInputs = new HashMap<>();
        Map<String, BigInteger> candidateMap = new HashMap<>();

        // 收集所有消息数据
        for (DataAccessMessage msg : roundData.messages.values()) {
            String nodeId = msg.from.toString();
            nodeScores.put(nodeId, msg.nodeScore);
            nodeProofs.put(nodeId, new NodeProof(msg.merkleRoot, msg.recordHashes));
            nodeVrfOutputs.put(nodeId, msg.vrfoutput.getRandomValue());
            nodeVrfInputs.put(nodeId, msg.input);

            // 合并统计信息
            msg.dataAccessCount.forEach((key, value) ->
                    globalAccessCounts.merge(key, value, Integer::sum));

            msg.dataAccessNodes.forEach((key, valueSet) ->
                    globalAccessNodes.computeIfAbsent(key, k -> new HashSet<>()).addAll(valueSet));
        }

        // 选举中心节点
        List<Map.Entry<String, Double>> sortedCandidates = new ArrayList<>(nodeScores.entrySet());
        sortedCandidates.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        Map<BigInteger, BigInteger> candidateVrfValues = new LinkedHashMap<>();
        sortedCandidates.stream()
                .limit(5)
                .forEach(entry -> {
                    String nodeId = entry.getKey();
                    candidateVrfValues.put(new BigInteger(nodeId), nodeVrfOutputs.get(nodeId));
                    candidateMap.put(nodeId, nodeVrfInputs.get(nodeId));
                });

        String newCentralNodeId = VRFElection.electNextCentralNode(candidateVrfValues);
        System.out.println("第 " + round + " 轮选出新中心节点: " + newCentralNodeId);

        // 计算数据评分
        Map<String, double[]> dataScores = DataActivityScore.calculateActivityScore(
                globalAccessCounts, globalAccessNodes,
                currentNode.getHistoryData(),
                currentNode.getDataScore()
        );
        System.out.println("globalAccessNodes: " + globalAccessNodes);
        dataScores.forEach((key, values) -> {
            System.out.println(key + " -> " + Arrays.toString(values));
        });
        // 打包并上传区块
        List<String> nodeRoots = nodeProofs.values().stream()
                .map(p -> p.merkleRoot)
                .collect(Collectors.toList());
        MerkleTree globalTree = new MerkleTree(nodeRoots);

        BlockData blockData = new BlockData(
                globalTree.getRootHash(),
                newCentralNodeId,
                candidateMap,
                dataScores,
                nodeScores,
                nodeProofs.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().merkleRoot))
        );

        Block block = new Block(currentNode.getBlockchain().getLastBlockHash(), blockData);
        currentNode.getBlockchain().broadcastBlock(block);

        // 最后移除本轮缓存（自动回收）
        roundBuffer.remove(round);
    }

    // 内部类用于证明结构
    private static class NodeProof {
        final String merkleRoot;
        final List<String> recordHashes;

        NodeProof(String merkleRoot, List<String> recordHashes) {
            this.merkleRoot = merkleRoot;
            this.recordHashes = new ArrayList<>(recordHashes);
        }
    }
}
