package vRouter;

import java.util.*;
import java.math.BigInteger;

import peersim.core.Network;

public class Blockchain {
    private List<Block> chain; // 区块链
    private String lastBlockHash; // 最后一个区块的哈希

    public Blockchain() {
        this.chain = new ArrayList<>();
    }
    public void forceSetGenesisBlock(Block genesisBlock) {
        if (chain.isEmpty()) {
            chain.add(genesisBlock);
        } else {
            chain.set(0, genesisBlock); // 替换现有的创世区块
        }
    }
    // 添加外部区块
    public void addBlock(Block block) {
        if (isBlockValid(block)) {
            chain.add(block);
            lastBlockHash = block.getBlockHash(); // 更新最后一个区块的哈希
        } else {
            throw new IllegalArgumentException("无效的区块");
        }
    }

    // 验证区块的有效性
    private boolean isBlockValid(Block block) {
        // 验证区块的哈希是否正确
        if (!block.getBlockHash().equals(block.calculateHash())) {
            return false;
        }

        // 验证区块的前一个哈希是否与本地链的最后一个区块哈希匹配
        Block lastBlock = getLastBlock();
        if (!block.getPreviousHash().equals(lastBlock.getBlockHash())) {
            return false;
        }
        return true;
    }


    // 获取最后一个区块
    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }
    public String getLastBlockHash(){
        return chain.get(chain.size() - 1).getBlockHash();
    }
    public String getCentralNodeId(){
        return chain.get(chain.size() - 1).getData().getCentralNodeId();
    }

    // 获取区块链
    public List<Block> getChain() {
        return chain;
    }

    // 模拟区块广播
    public void broadcastBlock(Block block) {
      //  System.out.println("广播新区块: " + block.getBlockHash());
        for (int i = 0; i < Network.size(); i++) {
            MyNode node = (MyNode) Network.get(i);  // 获取节点
            node.receiveBlock(block);
        }
    }
}
