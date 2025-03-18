package vRouter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import peersim.core.Network;

public class VRFElection {

    private static class VRFOutput {
        BigInteger randomValue; // 随机数
        String proof; // 证明

        public VRFOutput(BigInteger randomValue, String proof) {
            this.randomValue = randomValue;
            this.proof = proof;
        }

        public BigInteger getRandomValue() {
            return randomValue;
        }

        public String getProof() {
            return proof;
        }
    }

    // 模拟 VRF 计算
    private static VRFOutput computeVRF(BigInteger privateKey, BigInteger input) {
        try {
            // 使用 SHA-256 模拟 VRF
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = privateKey.toString() + input.toString();
            byte[] hashBytes = digest.digest(data.getBytes());

            // 将哈希值转换为 BigInteger
            BigInteger randomValue = new BigInteger(1, hashBytes);

            // 模拟证明（实际应用中需要使用 VRF 的证明算法）
            String proof = "proof-" + randomValue.toString();

            return new VRFOutput(randomValue, proof);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    // 验证 VRF 输出
    private static boolean verifyVRF(BigInteger publicKey, BigInteger input, BigInteger randomValue, String proof) {
        // 模拟验证（实际应用中需要使用 VRF 的验证算法）
        VRFOutput expectedOutput = computeVRF(publicKey, input);
        return expectedOutput.getRandomValue().equals(randomValue) && expectedOutput.getProof().equals(proof);
    }

    // 选举新的中心节点
    public static MyNode electNextCentralNode(List<BigInteger> candidateList) {
        if (candidateList.isEmpty()) {
            throw new IllegalArgumentException("候选节点集合不能为空");
        }

        // 模拟每个候选节点的 VRF 输出
        Map<BigInteger, VRFOutput> vrfOutputs = new HashMap<>();
        for (BigInteger candidate : candidateList) {
            BigInteger privateKey = candidate; // 假设私钥是节点 ID
            BigInteger input = candidateList.get(candidateList.size()/2); // 使用时间戳作为输入
            VRFOutput output = computeVRF(privateKey, input);
            vrfOutputs.put(candidate, output);
        }

        // 选择与节点 ID 最接近的 VRF 输出对应的节点
        BigInteger selectedCandidate = null;
        BigInteger minDifference = null; // 最小差距
        for (Map.Entry<BigInteger, VRFOutput> entry : vrfOutputs.entrySet()) {
            BigInteger randomValue = entry.getValue().getRandomValue();
            BigInteger nodeId = entry.getKey();

            // 计算 nodeId 和 randomValue 之间的差值
            BigInteger difference = randomValue.subtract(nodeId).abs(); // 绝对差值

            // 更新最小差距
            if (minDifference == null || difference.compareTo(minDifference) < 0) {
                minDifference = difference;
                selectedCandidate = nodeId;
            }
        }

        // 验证选举结果
        if (selectedCandidate != null) {
            VRFOutput selectedOutput = vrfOutputs.get(selectedCandidate);
            boolean isValid = verifyVRF(selectedCandidate,candidateList.get(candidateList.size()/2),
                    selectedOutput.getRandomValue(), selectedOutput.getProof());
            if (!isValid) {
                throw new RuntimeException("选举结果验证失败");
            }
        }

        // 返回新的中心节点
        for (int i = 0; i < Network.size(); i++) {
            MyNode node = (MyNode) Network.get(i);
            if (node.nodeId.equals(selectedCandidate)) {
                return node;
            }
        }
        throw new RuntimeException("未找到选定的中心节点");
    }
}