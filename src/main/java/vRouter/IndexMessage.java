package vRouter;

import java.math.BigInteger;

public class IndexMessage {
    public BigInteger dataID;
    public BigInteger from;
    public int hops;
    public BigInteger initial;

    public IndexMessage(BigInteger data, BigInteger origin){
        this.dataID = data;
        this.from = origin;
        this.hops = 1;
        this.initial = origin;
    }
//    模拟消息从一个节点转发到另一个节点，并增加跳数。
//    local：表示当前转发消息的节点 ID。
//    创建一个新的 IndexMessage 对象，保留原始的 dataID（数据 ID）。
//    将新的消息来源节点设置为 local，即当前节点。
//    将原消息的跳数加 1，表示该消息已经经过了一次转发。
//    返回值：一个新的 IndexMessage 对象，包含更新后的来源节点和跳数信息。
    public IndexMessage relay(BigInteger local){
        IndexMessage relay = new IndexMessage(dataID, local);
        relay.hops = this.hops+1;
        relay.initial = this.initial;
        return relay;
    }
}
