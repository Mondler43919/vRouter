package vRouter;

import java.math.BigInteger;
import java.util.Map;
import java.io.Serializable;

public class VRFElection {
    public static final int PRIVATE_KEY_SIZE = 32;
    public static final int PUBLIC_KEY_SIZE = 32;
    public static final int PROOF_SIZE = 96;

    static {
        System.load("D:\\vRouter\\lib\\secp256r1_hash.dll");
    }

    public static class KeyPair {
        private final byte[] privateKey;
        private final byte[] publicKey;

        public KeyPair(byte[] privateKey, byte[] publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }

        public byte[] getPrivateKey() {
            return privateKey.clone();
        }

        public byte[] getPublicKey() {
            return publicKey.clone();
        }
    }


    public static class VRFOutput implements Serializable{
        private static final long serialVersionUID = 1L;
        private final byte[] randomValue;
        private final byte[] proof;

        public VRFOutput(byte[] randomValue, byte[] proof) {
            this.randomValue = randomValue;
            this.proof = proof;
        }

        public byte[] getRandomValue() {
            return randomValue;
        }

        public byte[] getProof() {
            return proof;
        }
    }

    public static native KeyPair generateKeyPair();

    public static native VRFOutput generateVRF(byte[] privateKey, byte[] input);

    public static native boolean verifyVRFProof(byte[] publicKey, byte[] input, byte[] proof);

    public static String electNextCentralNode(Map<BigInteger, byte[]> candidateVrfValues) {
        if (candidateVrfValues == null || candidateVrfValues.isEmpty()) {
            return null;
        }

        BigInteger selectedNode = null;
        BigInteger minDistance = null;

        for (Map.Entry<BigInteger, byte[]> entry : candidateVrfValues.entrySet()) {
            BigInteger nodeId = entry.getKey();
            byte[] vrfOutput = entry.getValue();

            // 将VRF输出字节数组转换为BigInteger
            BigInteger vrfValue = new BigInteger(1, vrfOutput);

            // 计算节点ID与VRF值的距离（绝对值差）
            BigInteger distance = nodeId.subtract(vrfValue).abs();

            // 选择距离最小的节点
            if (minDistance == null || distance.compareTo(minDistance) < 0) {
                minDistance = distance;
                selectedNode = nodeId;
            }
        }

        return selectedNode != null ? selectedNode.toString() : null;
    }
}