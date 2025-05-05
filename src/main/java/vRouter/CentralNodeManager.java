package vRouter;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.*;


public class CentralNodeManager {
    private final MyNode currentNode;

    // 每轮缓存：key = round number
    private final Map<Long, RoundData> roundBuffer = new HashMap<>();
    private final Map<String, Double> nodeScores = new HashMap<>();
    private final Map<String, Integer> globalAccessCounts = new HashMap<>();
    private final Map<String, Set<String>> globalAccessNodes = new HashMap<>();
    private final Map<String, String> nodeProofs = new HashMap<>();
    private final List<String> nodeRootHashes = new ArrayList<>();
    private final Map<String, BigInteger> nodeVrfOutputs = new HashMap<>();
    private final Map<String, BigInteger> nodeVrfInputs = new HashMap<>();
    private final Map<String, BigInteger> candidateMap = new HashMap<>();

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
        //时间约束+收到节点消息
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

        if (roundData.received == Network.size()-1) { // -1 因为中心节点不发
            executeRound(round, roundData);
        }
    }

    // === 执行某一轮中心节点逻辑 ===
    private void executeRound(long round, RoundData roundData) {
        System.out.println("======= 执行第 " + round + " 轮 =======");

        for (String prefix : QueryGenerator.dataPrefixes) {
            globalAccessCounts.putIfAbsent(prefix, 0);
            globalAccessNodes.putIfAbsent(prefix, new HashSet<>());
        }
        // 收集所有消息数据
        for (DataAccessMessage msg : roundData.messages.values()) {
            String nodeId = msg.from.toString();
            nodeScores.put(nodeId, msg.nodeScore);
            nodeProofs.put(nodeId, msg.merkleRoot);
            nodeRootHashes.add(msg.merkleRoot);
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
        dataScores.forEach((key, values) -> {
            System.out.println(key + " -> " + Arrays.toString(values));
        });

        // DataVisualizationService visualizer = DataVisualizationService.getInstance();
        // visualizer.updatePerRound(
        //         round,
        //         globalAccessCounts,
        //         globalAccessNodes,
        //         dataScores
        // );

        //打包上链
        MerkleTree globalTreeFromNodeRoots = new MerkleTree(nodeRootHashes);
        // Map<String, List<String>> nodeProofPaths = new HashMap<>();
        // for (String nodeId : nodeProofs.keySet()) {
        //     String nodeRoot = nodeProofs.get(nodeId);
        //     int leafIndex = nodeRootHashes.indexOf(nodeRoot); // 查找该节点在根列表中的索引
        //     List<String> proofPath = globalTreeFromNodeRoots.getProofPath(leafIndex);
        //     nodeProofPaths.put(nodeId, proofPath);
        // }

         BlockData blockData = new BlockData(
                globalTreeFromNodeRoots.getRootHash(),
                newCentralNodeId,
                candidateMap,
                dataScores,
                nodeScores
                );

        Block block = new Block(currentNode.getBlockchain().getLastBlockHash(), blockData);

        // 记录首次接收时间（如果尚未记录）
        VRouterObserver.blockCreationTimes.putIfAbsent(
                block.getBlockHash(),
                round*1000 // 记录当前模拟时间为区块产生时间
        );

        VRouterProtocol p = (VRouterProtocol) currentNode.getProtocol(0);
        p.sendBlock2Neighbors(block);


        // 最后移除本轮缓存（自动回收）
        roundBuffer.remove(round);
        nodeScores.clear();
        globalAccessCounts.clear();
        globalAccessNodes.clear();
        nodeProofs.clear();
        nodeVrfOutputs.clear();
        nodeVrfInputs.clear();
        candidateMap.clear();
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
