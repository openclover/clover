package com.atlassian.clover.recorder;

import com.atlassian.clover.registry.CoverageDataRange;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

public interface PerTestCoverage {
    /** @return true when this instance contains per-test data */
    boolean hasPerTestData();

    /** @return a non-live set of tcis (ordered by addition) */
    Set<TestCaseInfo> getTests();

    int getCoverageSize();

    /** @return an unordered set of tcis that intersect with the given range */
    Set<TestCaseInfo> getTestsCovering(CoverageDataRange range);

    /** @return a map of TCIs->their coverage for the given FileInfo */
    Map<TestCaseInfo, BitSet> mapTestsAndCoverageForFile(FullFileInfo fileInfo);

    /** @return a test case identified by the id. null if no such test case can be found */
    TestCaseInfo getTestById(int id);

    /** @return a non-null BitSet for the hits of all tests*/
    BitSet getAllHits();

    /** @return a non-null BitSet for the hits of all tests which passed */
    BitSet getPassOnlyHits();

    /** @return a non-null BitSet for the hits of the test case */
    BitSet getHitsFor(TestCaseInfo tci);

    /** @return a non-null slot set with those that intersect the given slices/tcis set to true */
    BitSet getHitsFor(Set<TestCaseInfo> tcis);

    /** @return a non-null BitSet for the hits of the supplied TestCaseInfos restricted to the specified range */
    BitSet getHitsFor(Set<TestCaseInfo> tcis, CoverageDataRange range);

    /** @return a non-null BitSet for the unique hits of the supplied TestCaseInfo */
    BitSet getUniqueHitsFor(TestCaseInfo tci);

    /** @return a non-null BitSet for the unique hits of the supplied TestCaseInfos */
    BitSet getUniqueHitsFor(Set<TestCaseInfo> tcis);

    void addCoverage(TestCaseInfo tci, PerTestRecordingTranscript recording);
}
