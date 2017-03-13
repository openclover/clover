package com.atlassian.clover;

import com.atlassian.clover.registry.CoverageDataProvider;

import java.util.BitSet;

public class BitSetCoverageProvider extends BaseTCILookupStore implements CoverageDataProvider {
    private BitSet hits;

    public BitSetCoverageProvider(BitSet coverage, TCILookupStore store) {
        super(store.getTciLookups());
        this.hits = (BitSet)coverage.clone();
    }

    @Override
    public int getHitCount(int index) {
        return hits.get(index) ? 1 : 0;
    }

}
