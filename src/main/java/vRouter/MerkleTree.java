package vRouter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    private String rootHash;

    public MerkleTree(List<String> dataHashes) {
        this.rootHash = buildTree(dataHashes);
    }

    // 构建默克尔树
    private String buildTree(List<String> dataHashes) {
        if (dataHashes.isEmpty()) {
            return "";
        }

        List<String> currentLevel = new ArrayList<>(dataHashes);

        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                String combinedHash = calculateHash(left + right);
                nextLevel.add(combinedHash);
            }
            currentLevel = nextLevel;
        }

        return currentLevel.get(0);
    }

    // 计算哈希值
    private String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    // 将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // 获取根哈希
    public String getRootHash() {
        return rootHash;
    }
}
