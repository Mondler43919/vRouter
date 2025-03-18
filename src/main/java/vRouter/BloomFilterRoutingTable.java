package vRouter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BloomFilterRoutingTable {

    HashMap<BigInteger,ContactWithBloomFilter> bfRoutingTable;

    public void put(ContactWithBloomFilter contactBF){
        if(bfRoutingTable == null){
            bfRoutingTable = new HashMap<>();
        }
        bfRoutingTable.put(contactBF.contact,contactBF);
    }

    //根据节点获取对应的BloomFilter
    public ContactWithBloomFilter get(BigInteger node){
        if(bfRoutingTable == null){
            return null;
        }
        return bfRoutingTable.get(node);
    }

    //根据数据ID获取匹配BloomFilter的目标节点
    public List<BigInteger> getMatch(BigInteger dataID){
        if(bfRoutingTable == null){
            return null;
        }
        List<BigInteger> matchNodes = new ArrayList<>();

        for (ContactWithBloomFilter c: bfRoutingTable.values()) {
            if(c.contain(dataID)) matchNodes.add(c.contact);
        }
        return matchNodes;
    }
}
