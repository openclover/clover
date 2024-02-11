package org.openclover.core;

import java.util.concurrent.ConcurrentMap;

/**
 * Something that stores / retrieves {@link TestCaseInfoLookup}s by name. Used primarily to associate a particular
 * version of ClassInfo with the {@link TestCaseInfoLookup}s it defined.
 */
public interface TCILookupStore {
    TestCaseInfoLookup namedTCILookupFor(String name);

    ConcurrentMap<String, TestCaseInfoLookup> getTciLookups();
}
