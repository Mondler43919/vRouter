package vRouter;
//缓存增加
import java.math.BigInteger;

public class DataResponseMessage {
    public BigInteger dataID;
    public int data;
    public BigInteger from; // 发送者
    public BigInteger to;   // 目标节点（initialRequester）

       public int frequency;
        public  int distance;
        public int activityScore;
    public long initialTime;   // 初始时间（查找开始的时间）

    public DataResponseMessage(BigInteger dataID, int data, BigInteger from, BigInteger to, int frequency, int distance, int activityScore,long initialTime) {
        this.dataID = dataID;
        this.data = data;
        this.from = from;
        this.to = to;
        this.frequency = frequency;
        this.distance = distance;
        this.activityScore = activityScore;
        this.initialTime = initialTime;
    }
}
