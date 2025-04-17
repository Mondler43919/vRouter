package vRouter;

import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    private final List<String> leaves;
    private final String root;
    private final List<List<String>> treeLevels; // 存储所有层级用于生成证明路径

    public MerkleTree(List<String> leaves) {
        this.leaves = new ArrayList<>(leaves);
        this.treeLevels = new ArrayList<>();
        this.root = buildTree(this.leaves);
    }

    private String buildTree(List<String> leaves) {
        if (leaves.isEmpty()) {
            return "";
        }

        List<String> currentLevel = new ArrayList<>(leaves);
        treeLevels.add(new ArrayList<>(currentLevel));

        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                nextLevel.add(HashUtils.SHA256(left + right));
            }
            treeLevels.add(new ArrayList<>(nextLevel));
            currentLevel = nextLevel;
        }
        return currentLevel.get(0);
    }

    public String getRootHash() {
        return root;
    }

    public List<String> getProofPath(int leafIndex) {
        List<String> path = new ArrayList<>();
        int currentIndex = leafIndex;

        for (int level = 0; level < treeLevels.size() - 1; level++) {
            List<String> levelNodes = treeLevels.get(level);
            boolean isRightNode = (currentIndex % 2) == 1;
            int siblingIndex = isRightNode ? currentIndex - 1 : currentIndex + 1;

            if (siblingIndex < levelNodes.size()) {
                path.add(levelNodes.get(siblingIndex));
            }

            currentIndex /= 2;
        }

        return path;
    }

    public static boolean verify(String leaf, List<String> path, String root) {
        String currentHash = leaf;
        for (String sibling : path) {
            currentHash = HashUtils.SHA256(currentHash + sibling);
        }
        return currentHash.equals(root);
    }
}