package org.openclover.core;

import org.openclover.core.registry.CoverageDataProvider;

import java.util.BitSet;
import java.util.concurrent.ConcurrentMap;

/**
 * A coverage data provider which returns hits count reading it from <pre>store</pre>, but only when corresponding bit
 * in <pre>coverageMask</pre> is set; otherwise it returns 0. In addition to this, it can search for test cases.
 * This coverage provider is useful for applying a mask on hits data (for example to get hits only for single test case).
 */
public class MaskedBitSetCoverageProvider implements CoverageDataProvider, TCILookupStore {
    private BitSet hits;
    private TCILookupStore tciLookup;
    private CoverageDataProvider dataProvider;

    public MaskedBitSetCoverageProvider(BitSet coverageMask, TCILookupStore tciLookup, CoverageDataProvider dataProvider) {
        this.hits = (BitSet)coverageMask.clone();
        this.tciLookup = tciLookup;
        this.dataProvider = dataProvider;
    }

    @Override
    public int getHitCount(int index) {
        return hits.get(index) ? dataProvider.getHitCount(index) : 0;
    }


    @Override
    public TestCaseInfoLookup namedTCILookupFor(String name) {
        return tciLookup.namedTCILookupFor(name);
    }

    @Override
    public ConcurrentMap<String, TestCaseInfoLookup> getTciLookups() {
        return tciLookup.getTciLookups();
    }
}
