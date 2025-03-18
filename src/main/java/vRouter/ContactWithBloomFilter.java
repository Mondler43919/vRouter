package vRouter;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider;
import orestes.bloomfilter.memory.BloomFilterMemory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ContactWithBloomFilter {
    final BigInteger contact;
    List<BloomFilter<BigInteger>> bloomFilterList;

    public ContactWithBloomFilter(BigInteger income){
        contact = income;
    }

    public void add(BigInteger dataID){
        if(bloomFilterList == null){
            bloomFilterList = new ArrayList<>();
        }
        if(contain(dataID)) return;
        for (BloomFilter<BigInteger> bf: bloomFilterList) {
            if(bf.getEstimatedPopulation() < bf.getExpectedElements()){
                bf.add(dataID);
                return;
            }
//            System.out.println("=======overflow");
        }
        BloomFilter<BigInteger> bf = new BloomFilterMemory<>(new FilterBuilder(VRouterCommonConfig.EXPECTED_ELEMENTS, VRouterCommonConfig.FALSE_POSITIVE_PROB).hashFunction
                (HashProvider.HashMethod.MD5).complete());
        bf.add(dataID);
//        System.err.println("BF vector size: " + bf.getSize());
        VRouterObserver.bloomFilterCount.add(1);
        bloomFilterList.add(bf);
    }

    public boolean contain(BigInteger dataID){
        if(bloomFilterList == null){
            return false;
        }
        for (BloomFilter<BigInteger> bf: bloomFilterList) {
            if(bf.contains(dataID)) return true;
        }
        return false;
    }
}
