package vRouter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerkleTree {
    private final List<String> leaves;
    private final String root;
    private final List<List<String>> treeLevels; // 存储所有层级用于生成证明路径
    private final Map<Integer, List<String>> proofPaths; // 存储每个叶子节点的证明路径

    public MerkleTree(List<String> leaves) {
        this.leaves = new ArrayList<>(leaves);
        this.treeLevels = new ArrayList<>();
        this.proofPaths = new HashMap<>();
        this.root = buildTree(this.leaves);
    }

    // 构建树的同时生成每个叶子节点的证明路径
    private String buildTree(List<String> leaves) {
        if (leaves.isEmpty()) {
            return "";
        }

        List<String> currentLevel = new ArrayList<>(leaves);
        treeLevels.add(new ArrayList<>(currentLevel));

        // 在每一层构建时，也记录下证明路径
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                String parentHash = HashUtils.SHA256(left + right);
                nextLevel.add(parentHash);

                // 记录证明路径
                for (int j = 0; j < 2; j++) {
                    int index = i + j;
                    if (index < currentLevel.size()) {
                        proofPaths.putIfAbsent(index, new ArrayList<>());
                        proofPaths.get(index).add(j == 0 ? right : left); // 添加兄弟节点到路径中
                    }
                }
            }
            treeLevels.add(new ArrayList<>(nextLevel));
            currentLevel = nextLevel;
        }
        return currentLevel.get(0);
    }

    public String getRootHash() {
        return root;
    }

    // 获取某个叶子节点的证明路径
    public List<String> getProofPath(int leafIndex) {
        return proofPaths.getOrDefault(leafIndex, new ArrayList<>());
    }

    // 验证某个叶子节点的哈希是否正确
    public static boolean verify(String leaf, List<String> path, String root) {
        String currentHash = leaf;
        for (String sibling : path) {
            currentHash = HashUtils.SHA256(currentHash + sibling);
        }
        return currentHash.equals(root);
    }
}
