package kademlia;

import java.math.BigInteger;

public class LookupMessage{
    BigInteger target;
    BigInteger from;
    public int hops;
    public LookupMessage(BigInteger t,BigInteger f){
        target = t;
        from = f;
        hops = 1;
    }

    public LookupMessage nextHop(BigInteger f){
        LookupMessage msg = new LookupMessage(this.target,f);
        msg.hops = this.hops +1;
        return msg;
    }
}
