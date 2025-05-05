package vRouter;

import peersim.core.GeneralNode;
import peersim.config.Configuration;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.security.*;

public class MyNode extends GeneralNode{
    private final static String PAR_PROT = "protocol";
    private final int pid;
    private static String prefix ="vRouter";
    public boolean alreadyUpdate;
    private double nodeScore;
    private HashMap<String, double[]> dataScore;  // 记录数据评分

    public BigInteger nodeId;
    private Blockchain blockchain;
    private CentralNodeManager centralNodeManager;
    private HashMap<String, int[][]> historyData;
    private Integer cycle;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public MyNode(String prefix) {
        super(prefix);  // 调用 GeneralNode 的构造方法
        pid = Configuration.getPid(prefix + "." + PAR_PROT);

        nodeScore = 0.0;
        dataScore = new HashMap<>();

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
        clone.dataScore = new HashMap<>();  // 深拷贝 dataScore
        clone.blockchain = new Blockchain();  // 每个节点拥有独立的区块链
        clone.centralNodeManager = new CentralNodeManager(clone);  // 深拷贝 centralNodeManager
        clone.historyData=new HashMap<>();
        VRFKeyPair keyPair = new VRFKeyPair();
        clone.privateKey = keyPair.getPrivateKey();
        clone.publicKey = keyPair.getPublicKey();

        return clone;
    }

    public BigInteger getId() {
        VRouterProtocol p = (VRouterProtocol) this.getProtocol(pid);
        return p.nodeId;
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
            System.out.println("区块前哈希与链的最后区块的哈希不对");
            return false;
        }
        return true;
    }

    private void updateCachesWithBlockData(HashMap<String, double[]> blockDataScores) {
        final int TOTAL_BITS = 128;         // 数据ID总位数（128位）
        final int PREFIX_BITS = 56;         // 前缀二进制位数
        final int HEX_DIGITS = PREFIX_BITS / 4; // 前缀十六进制字符数（14）

        // 获取协议实例和缓存管理器
        VRouterProtocol protocol = (VRouterProtocol) this.getProtocol(pid);
        CacheEvictionManager cacheManager = protocol.cacheEvictionManager;

        // 遍历所有缓存项（键为BigInteger的字符串形式）
        Set<String> cachedKeys = new HashSet<>(cacheManager.getCachedDataKeys());
        for (String cachedKey : cachedKeys) {
            try {
                // 将字符串键转换为BigInteger
                BigInteger dataId = new BigInteger(cachedKey);
                // System.out.println(dataId);
                // 转换为128位二进制字符串（补前导零）
                String binaryStr = String.format("%128s", dataId.toString(2)).replace(' ', '0');

                // 3. 提取前56位二进制前缀
                String binaryPrefix = binaryStr.substring(0, PREFIX_BITS);

                // 4. 转换为14字符十六进制
                String hexPrefix = binaryToHex(binaryPrefix, HEX_DIGITS);
                //System.out.println(hexPrefix);
                // 5. 匹配评分数据
                if (blockDataScores.containsKey(hexPrefix)) {
                    double[] metrics = blockDataScores.get(hexPrefix);
                    if (metrics == null || metrics.length < 5) continue;

                    // 6. 更新缓存
                    int activeStatus = (int) metrics[4];
                    if (activeStatus == 0) {
                        cacheManager.evictByDataId(cachedKey);
                        if(dataId==null||QueryGenerator.dataRegistry.get(dataId)==null){
                            System.err.println("lalalalla");
                        }

                    } else {
                        cacheManager.updateCacheEntry(cachedKey, metrics[0]);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("无效的缓存键格式: " + cachedKey);
            }
        }
    }

    // 辅助方法：二进制字符串转十六进制（自动补零）
    private String binaryToHex(String binaryStr, int targetHexLength) {
        // 将二进制字符串转换为BigInteger
        BigInteger numericValue = new BigInteger(binaryStr, 2);
        // 转换为十六进制并补零
        return String.format("%" + targetHexLength + "s", numericValue.toString(16))
                .replace(' ', '0')
                .toLowerCase();
    }

    private void setHistoryData(Map<String, double[]> dataScores) {
        long currentCycle = peersim.core.CommonState.getTime()/cycle;
        int index =(int)(currentCycle % 10); // 计算当前周期的存储索引

        // 遍历区块数据，将其存入 historyData
        for (Map.Entry<String, double[]> entry : dataScores.entrySet()) {
            String dataId = entry.getKey();
            int accessCount = (int)entry.getValue()[0]; // 访问次数
            int activityLevel =(int) entry.getValue()[3]; // 活跃度等级

            // 如果 historyData 中没有该数据，初始化 2*10 数组
            historyData.putIfAbsent(dataId, new int[2][10]);

            // 更新该数据 ID 对应的数组
            historyData.get(dataId)[0][index] = accessCount;   // 存入访问次数
            historyData.get(dataId)[1][index] = activityLevel; // 存入活跃度等级
        }
    }

    public HashMap<String, int[][]> getHistoryData() {
        return historyData;
    }

    public void setNodeId(BigInteger tmp) {
        nodeId = tmp;
    }
    public void setDataScore(HashMap<String, double[]> dataScore) {
        this.dataScore = dataScore;
    }

    // 获取本地区块链
    public Blockchain getBlockchain() {
        return blockchain;
    }
    public double getNodeScore() {
        return nodeScore;
    }
    public HashMap<String, double[]> getDataScore() {
        return dataScore;
    }
    public CentralNodeManager getCentralNodeManager() {
        return centralNodeManager;
    }
    public VRFElection.VRFOutput generateVRFOutput(BigInteger input) {
        return VRFElection.computeVRF(this.privateKey, input);
    }
    public PublicKey getPublicKey() {
        return publicKey;
    }
}

