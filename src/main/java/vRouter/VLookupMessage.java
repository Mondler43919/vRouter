package vRouter;
//增加了时间和消息发送的初始节点initialRequester
import java.math.BigInteger;

//封装查找消息，它代表一个查找请求（或应答）在网络中的传播状态。
public class VLookupMessage {
    public BigInteger dataID;  // 数据 ID，用于查找的数据
    public BigInteger from;    // 消息发送方的节点
public BigInteger initialRequester;
    public long initialTime;   // 初始时间（查找开始的时间）
    public int forwardHops;    // 向前传递的跳数
    public int backwardHops;   // 向后传递的跳数
    public boolean direction;  // 消息方向，true 表示向前，false 表示向后

    // 构造函数，初始化数据ID、发送者ID，跳数，方向
    public VLookupMessage(BigInteger dataID, BigInteger initialRequester, boolean direction,
                          int forwardHops, int backwardHops, long initialTime) {
        this.dataID = dataID;
        this.initialRequester = initialRequester;
        this.direction = direction;
        this.forwardHops = forwardHops;
        this.backwardHops = backwardHops;
        this.initialTime = initialTime;
        this.from=initialRequester;
    }

    public VLookupMessage(BigInteger t, BigInteger from){
        dataID = t;  // 设置数据 ID
        this.from = from;  // 设置发送者节点 ID
        this.initialRequester = from;
        this.initialTime =  System.currentTimeMillis();; // 记录初始时间
        forwardHops = 1;  // 初始时，跳数为 1
        backwardHops = 0; // 初始时，回退跳数为 0
        direction = true; // 默认方向为正向
    }

    // 创建一个新的正向传递的消息，更新跳数
    public VLookupMessage forward(BigInteger from){
        VLookupMessage msg = new VLookupMessage(this.dataID, from);  // 创建新的消息对象
        msg.initialRequester = this.initialRequester;
        msg.forwardHops = this.forwardHops + 1;  // 向前跳数加 1
        msg.initialTime = this.initialTime; // 继承初始时间
        return msg;  // 返回新的向前传递的消息
    }

    // 创建一个新的反向传递的消息，更新跳数和方向
    public VLookupMessage backward(BigInteger from){
        VLookupMessage msg = new VLookupMessage(this.dataID, from);  // 创建新的消息对象
        msg.initialRequester = this.initialRequester;
        msg.backwardHops = this.backwardHops + 1;  // 向后跳数加 1
        msg.initialTime = this.initialTime; // 继承初始时间
        msg.direction = false;  // 设置方向为向后
        return msg;  // 返回新的向后传递的消息
    }
}
