package vRouter;

import java.math.BigInteger;
import java.security.*;
import java.io.Serializable;
import java.util.*;

import peersim.core.Network;

public class VRFElection {

    // **VRF 计算结果类**
    public static class VRFOutput implements Serializable {
        private static final long serialVersionUID = 1L;  // 添加 serialVersionUID

        private final BigInteger randomValue; // 伪随机数
        private final String proof; // 证明字符串

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

    // 仅使用公钥进行 VRF 计算
    public static VRFOutput computeVRF(PublicKey publicKey, BigInteger input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = publicKey.getEncoded(); // 获取公钥的字节数组
            byte[] inputData = (Base64.getEncoder().encodeToString(keyBytes) + "-" + input.toString()).getBytes();
            byte[] hashBytes = digest.digest(inputData);

            BigInteger randomValue = new BigInteger(1, hashBytes);
            String proof = Base64.getEncoder().encodeToString(hashBytes);

            return new VRFOutput(randomValue, proof);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    public static VRFOutput computeVRF(PrivateKey privateKey, BigInteger input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = privateKey.getEncoded(); // 获取公钥的字节数组
            byte[] inputData = (Base64.getEncoder().encodeToString(keyBytes) + "-" + input.toString()).getBytes();
            byte[] hashBytes = digest.digest(inputData);

            BigInteger randomValue = new BigInteger(1, hashBytes);
            String proof = Base64.getEncoder().encodeToString(hashBytes);

            return new VRFOutput(randomValue, proof);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    // 验证VRF结果
    public static boolean verifyVRF(PublicKey vk, BigInteger input, BigInteger randomValue, String proof) {
        VRFOutput expectedOutput = computeVRF(vk, input);
        return expectedOutput.getRandomValue().equals(randomValue) &&
                expectedOutput.getProof().equals(proof);
    }

    public static String electNextCentralNode(Map<BigInteger, BigInteger> candidateVrfValues) {

        BigInteger selectedNode = null;
        BigInteger minDifference = null;

        for (Map.Entry<BigInteger, BigInteger> entry : candidateVrfValues.entrySet()) {
            BigInteger nodeId = entry.getKey();
            BigInteger vrfValue = entry.getValue();
            BigInteger difference = nodeId.subtract(vrfValue).abs();

            if (minDifference == null || difference.compareTo(minDifference) < 0) {
                minDifference = difference;
                selectedNode = nodeId;
            }
        }
        return selectedNode.toString();
    }

}
