package vRouter;

import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    private final MerkleNode root; // 根节点

    // Merkle 树节点定义
    private static class MerkleNode {
        private final String hash;  // 当前节点的哈希值
        private MerkleNode left;   // 左子节点
        private MerkleNode right;  // 右子节点

        public MerkleNode(String hash) {
            this.hash = hash;
        }

        // 判断是否为叶子节点（无子节点）
        public boolean isLeaf() {
            return left == null && right == null;
        }

        // Getter 方法
        public String getHash() { return hash; }
        public MerkleNode getLeft() { return left; }
        public MerkleNode getRight() { return right; }
    }

    // 构造函数：从叶子节点哈希列表构建 Merkle 树
    public MerkleTree(List<String> leaves) {
        this.root = buildTree(leaves);
    }

    // 构建 Merkle 树的核心逻辑
    private MerkleNode buildTree(List<String> leaves) {
        if (leaves.isEmpty()) {
            return null; // 空树
        }

        // 第一层：所有叶子节点
        List<MerkleNode> currentLevel = new ArrayList<>();
        for (String leaf : leaves) {
            currentLevel.add(new MerkleNode(leaf));
        }

        // 逐层向上构建父节点，直到根节点
        while (currentLevel.size() > 1) {
            List<MerkleNode> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                MerkleNode left = currentLevel.get(i);
                MerkleNode right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left; // 奇数节点时复制左节点
                String parentHash = HashUtils.SHA256(left.getHash() + right.getHash()); // 计算父节点哈希
                MerkleNode parent = new MerkleNode(parentHash);
                parent.left = left;
                parent.right = right;
                nextLevel.add(parent);
            }
            currentLevel = nextLevel;
        }
        return currentLevel.get(0); // 返回根节点
    }

    // 获取根节点哈希
    public String getRootHash() {
        return root != null ? root.getHash() : "";
    }

    // 获取根节点（允许外部遍历树结构）
    public MerkleNode getRoot() {
        return root;
    }
}