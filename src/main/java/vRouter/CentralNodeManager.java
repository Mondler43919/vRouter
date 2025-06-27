package vRouter;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.*;


public class CentralNodeManager {
    private final MyNode currentNode;

    private final Map<Long, RoundData> roundBuffer = new HashMap<>();
    private  final  int cyclelength=Configuration.getInt("CYCLE");
    private final Map<String, Double> nodeScores = new HashMap<>();
    private final Map<String, Integer> globalAccessCounts = new HashMap<>();
    private final Map<String, Set<String>> globalAccessNodes = new HashMap<>();
    private final Map<String, String> nodeProofs = new HashMap<>();
    private final List<String> nodeRootHashes = new ArrayList<>();
    private final Map<String, byte[]> nodeVrfOutputs = new HashMap<>();
    private final Map<String, byte[]> nodeVrfInputs = new HashMap<>();
    private final Map<String, byte[]> candidateMap = new HashMap<>();

    public CentralNodeManager(MyNode currentNode) {
        this.currentNode = currentNode;
    }

    // 所有数据
    private static class RoundData {
        int received = 0;
        final Map<String, DataAccessMessage> messages = new HashMap<>();
    }

    public void processDataAccessMessage(DataAccessMessage message) {
        long round = message.cycle; // 消息携带的轮次
        String senderId = message.from.toString();

        // 拿到或创建该轮的 RoundData
        roundBuffer.putIfAbsent(round, new RoundData());
        RoundData roundData = roundBuffer.get(round);

        // 如果已收到该节点消息就跳过
        if (roundData.messages.containsKey(senderId)) return;

        // 存入消息
        roundData.messages.put(senderId, message);
        roundData.received++;

        if (roundData.received == Network.size()*0.9) {
            executeRound(round, roundData);
        }
    }

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

        Map<BigInteger, byte[]> candidateVrfValues = new LinkedHashMap<>();
        sortedCandidates.stream()
                .limit(10)
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
                currentNode.getDataScore(),
                round
        );
        dataScores.forEach((key, values) -> {
            System.out.println(key + " -> " + Arrays.toString(values));
        });

        DataVisualizationService visualizer = DataVisualizationService.getInstance();
        visualizer.updatePerRound(
                round,
                globalAccessCounts,
                globalAccessNodes,
                dataScores
        );

        //打包上链
        MerkleTree globalTree = new MerkleTree(nodeRootHashes);

         BlockData blockData = new BlockData(
                globalTree.getRootHash(),
                nodeRootHashes,
                newCentralNodeId,
                candidateMap,
                dataScores,
                nodeScores
                );

        Block block = new Block(currentNode.getBlockchain().getLastBlockHash(), blockData);
        int blocksize=block.cachedSize;

        currentNode.getBlockchain().broadcastBlock(block);

        long propagationTime = CommonState.getTime() - round*cyclelength;
        VRouterObserver.blockPropagationTime.add(propagationTime);
        VRouterObserver.blockSize.add(blocksize);
        System.out.println("产块时间： "+propagationTime+"  区块大小："+blocksize);

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

}
