package vRouter;

import peersim.core.GeneralNode;
import peersim.core.Network;
import peersim.config.Configuration;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.*;

public class MyNode extends GeneralNode{
    private final static String PAR_PROT = "protocol";
    private final int pid;
    private static String prefix ="vRouter";
    public boolean alreadyUpdate;
    private boolean[] isCentralNode;
    private double nodeScore;
    private HashMap<BigInteger, double[]> dataScore;  // 记录数据评分
    private HashMap<String, Object> dataMetrics;    //评分依据

    public BigInteger nodeId;
    private Blockchain blockchain;
    private CentralNodeManager centralNodeManager;
    private HashMap<BigInteger, int[][]> historyData;
    private Integer cycle;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public MyNode(String prefix) {
        super(prefix);  // 调用 GeneralNode 的构造方法
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        isCentralNode = new boolean[5];

        nodeScore = 0.0;
        dataScore = new HashMap<>();
        dataMetrics = new HashMap<>();

        blockchain = new Blockchain();
        centralNodeManager = new CentralNodeManager(this);
        alreadyUpdate=false;
        historyData = new HashMap<>();
        cycle= Configuration.getInt("CYCLE");
        VRFKeyPair keyPair = new VRFKeyPair();
        this.privateKey = keyPair.getPrivateKey();
        this.publicKey = keyPair.getPublicKey();
    }
    @Override
    public MyNode clone() {
        MyNode clone = (MyNode) super.clone();  // 调用父类的 clone 方法
        // 深拷贝需要独立的对象
        clone.isCentralNode = new boolean[5];
        clone.dataScore = new HashMap<>();  // 深拷贝 dataScore
        clone.dataMetrics = new HashMap<>();  // 深拷贝 dataMetrics
        clone.blockchain = new Blockchain();  // 每个节点拥有独立的区块链
        clone.centralNodeManager = new CentralNodeManager(clone);  // 深拷贝 centralNodeManager
        clone.historyData=new HashMap<>();
        VRFKeyPair keyPair = new VRFKeyPair();
        clone.privateKey = keyPair.getPrivateKey();
        clone.publicKey = keyPair.getPublicKey();

        return clone;
    }

    public void updateData() {
        VRouterProtocol p = (VRouterProtocol) this.getProtocol(pid);
        this.dataMetrics =p.getDataMetrics();
        this.nodeScore = NodeActivityScore.calculateActivityScore(dataMetrics);
        alreadyUpdate=true;
    }
    public BigInteger getId() {
        VRouterProtocol p = (VRouterProtocol) this.getProtocol(pid);
        return p.nodeId;
    }

    // 计算数据的哈希值
    public String calculateDataHash(HashMap<String, Object> dataMetrics) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String dataString = dataMetrics.toString(); // 将数据转换为字符串
            byte[] hashBytes = digest.digest(dataString.getBytes());
            return bytesToHex(hashBytes); // 将字节数组转换为十六进制字符串
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

    public void receiveBlock(Block block) {
        // 验证区块的有效性
        if (isBlockValid(block)) {
            // 如果区块有效，添加到本地区块链
            blockchain.addBlock(block);
            //更新数据评分
            setDataScore(((BlockData)block.getData()).getDataScores());
            setHistoryData(dataScore);
            updateCachesWithBlockData(((BlockData) block.getData()).getDataScores());
        } else {
            System.out.println("节点 " + nodeId + " 接收到无效区块: " + block.getBlockHash());
        }
    }
    private void updateCachesWithBlockData(HashMap<BigInteger, double[]> blockDataScores) {
        // 遍历网络中的所有节点
        for (int i = 0; i < Network.size(); i++) {
            MyNode node = (MyNode) Network.get(i);
            // 获取节点的协议实例
            VRouterProtocol protocol = (VRouterProtocol) node.getProtocol(Configuration.getPid("vRouter.protocol"));
            CacheEvictionManager cacheManager = protocol.cacheEvictionManager;

            // 获取该节点缓存中的所有数据ID（原始键，如 "123"）
            Set<String> cachedKeys = cacheManager.getCachedDataKeys();

            // 遍历缓存中的每个数据ID
            for (String cachedKey : cachedKeys) {
                BigInteger dataId = new BigInteger(cachedKey);
                // 如果该数据ID在区块的评分中存在
                if (blockDataScores.containsKey(dataId)) {
                    double[] metrics = blockDataScores.get(dataId);
                    double newActivityScore = metrics[0]; // 平滑后的评分（索引0）
                    int activeStatus = (int) metrics[4];    // 活跃状态（索引4）

                    if (activeStatus == 0) {
                        // 删除不活跃的数据
                        cacheManager.evictByDataId(cachedKey);
                    } else {
                        // 更新缓存中的activityScore
                        cacheManager.updateCacheEntry(cachedKey, newActivityScore);
                    }
                }
            }
        }
    }

    // 验证区块的有效性
    private boolean isBlockValid(Block block) {
        // System.out.println("区块哈希："+ block.getBlockHash() + "计算哈希" + block.calculateHash());
        // 验证区块的哈希是否正确
        if (!block.getBlockHash().equals(block.calculateHash())) {
            System.out.println("区块哈希不对");
            return false;
        }
        Block lastBlock = blockchain.getLastBlock();
        if (!block.getPreviousHash().equals(lastBlock.getBlockHash())) {
            return false;
        }
        return true;
    }
    private void setHistoryData(Map<BigInteger, double[]> dataScores) {
        long currentCycle = peersim.core.CommonState.getTime()/cycle;
        int index =(int)(currentCycle % 10); // 计算当前周期的存储索引

        // 遍历区块数据，将其存入 historyData
        for (Map.Entry<BigInteger, double[]> entry : dataScores.entrySet()) {
            BigInteger dataId = entry.getKey();
            int accessCount = (int)entry.getValue()[0]; // 访问次数
            int activityLevel =(int) entry.getValue()[1]; // 活跃度等级

            // 如果 historyData 中没有该数据，初始化 2*10 数组
            historyData.putIfAbsent(dataId, new int[2][10]);

            // 更新该数据 ID 对应的数组
            historyData.get(dataId)[0][index] = accessCount;   // 存入访问次数
            historyData.get(dataId)[1][index] = activityLevel; // 存入活跃度等级
        }

        // 遍历 historyData，将 `BlockData` 未包含的数据置 0
        for (Map.Entry<BigInteger, int[][]> entry : historyData.entrySet()) {
            BigInteger dataId = entry.getKey();
            int[][] dataArray = entry.getValue();

            // 如果该数据 ID 不在 `BlockData` 中，置 0
            if (!dataScores.containsKey(dataId)) {
                dataArray[0][index] = 0; // 访问次数置 0
                dataArray[1][index] = 0; // 活跃度等级置 0
            }
        }
    }

    public HashMap<BigInteger, int[][]> getHistoryData() {
        return historyData;
    }

    public void setNodeId(BigInteger tmp) {
        nodeId = tmp;
    }
    public void setDataScore(HashMap<BigInteger, double[]> dataScore) {
        this.dataScore = dataScore;
    }
    public void setAsCentralNode(boolean isCentralNode, long cycle) {
        int index = (int) (cycle % 5);  // 计算索引
        this.isCentralNode[index] = isCentralNode;
    }

    // 获取本地区块链
    public Blockchain getBlockchain() {
        return blockchain;
    }
    public HashMap<String, Object> getDataMetrics(){
        if(!alreadyUpdate) {
            updateData();
            alreadyUpdate=false;
        }
        return dataMetrics;
    }
    public double getNodeScore() {
        return nodeScore;
    }
    public HashMap<BigInteger, double[]> getDataScore() {
        return dataScore;
    }
    public boolean getIsCentralNode(long cycle) {
        int index = (int) (cycle % 5);  // 计算索引
        return this.isCentralNode[index];
    }
    public CentralNodeManager getCentralNodeManager() {
        return centralNodeManager;
    }
    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    // public VRFElection.VRFOutput generateVRFOutput(BigInteger input) {
    //     return VRFElection.computeVRF(this.privateKey, input);
    // }
    public PublicKey getPublicKey() {
        return publicKey;
    }

}

