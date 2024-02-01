package com.atlassian.clover;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseTCILookupStore implements TCILookupStore {
    private final ConcurrentMap<String, TestCaseInfoLookup> tciLookups;

    public BaseTCILookupStore() {
        this(new ConcurrentHashMap<>());
    }
    
    public BaseTCILookupStore(ConcurrentMap<String, TestCaseInfoLookup> tciLookups) {
        this.tciLookups = tciLookups;
    }

    @Override
    public TestCaseInfoLookup namedTCILookupFor(String name) {
        TestCaseInfoLookup lookup = tciLookups.get(name);
        if (lookup == null) {
            lookup = new TestCaseInfoLookup();
            tciLookups.putIfAbsent(name, lookup);
        }
        return tciLookups.get(name);
    }

    @Override
    public ConcurrentMap<String, TestCaseInfoLookup> getTciLookups() {
        return tciLookups;
    }
}
