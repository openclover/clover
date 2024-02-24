package org.openclover.core;

import org.openclover.core.cfg.StorageSize;
import org.openclover.core.recorder.GlobalCoverageRecordingTranscript;
import org.openclover.core.recorder.PerTestCoverage;
import org.openclover.core.recorder.PerTestRecordingTranscript;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.api.registry.CoverageDataRange;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.CloverBitSet;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Sets.newHashSet;

public class CoverageData extends BaseTCILookupStore implements ApplicationCoverage, PerTestCoverage {
    public static final StorageSize DEFAULT_EST_PER_TEST_COV_SIZE = StorageSize.fromString("256m");
    public static final int DEFAULT_EST_PER_TEST_RECORDINGS = 1000;

     /**
      * Coverage slots, for accumulated coverage counts. Each index represents
      * a programming construct detected during instrumentation (statement,
      * method etc) and each value represents the number of hits.
      */
    private final int [] hitCounts;
    private final PerTestCoverage perTestCoverage;
    private boolean empty;
    private long timestamp;
    private final long registryVersion;

    public CoverageData(Clover2Registry registry) {
        this(registry, new CoverageDataSpec());
    }

    public CoverageData(Clover2Registry registry, CoverageDataSpec spec) {
        this(registry, spec, DEFAULT_EST_PER_TEST_RECORDINGS);
    }

    public CoverageData(long timestamp, int[] hitCounts, PerTestCoverage perTestCoverage) {
        this.timestamp = timestamp;
        this.hitCounts = hitCounts;
        this.perTestCoverage = perTestCoverage;
        this.registryVersion = 0;
        this.empty = false;
    }

    private CoverageData(Clover2Registry registry, CoverageDataSpec spec, int estPerTestRecordings) {
        hitCounts = new int[registry.getDataLength()];
        registryVersion = registry.getVersion();
        perTestCoverage = spec.getPerTestStrategy().build(registry, spec, estPerTestRecordings);
        empty = true;
    }

    CoverageData(Clover2Registry registry, CoverageData other, CoverageDataSpec spec) {
        super(other.getTciLookups());
        timestamp = other.timestamp;
        hitCounts = new int[registry.getDataLength()];
        System.arraycopy(other.hitCounts, 0, hitCounts, 0, Math.min(other.hitCounts.length, hitCounts.length));
        registryVersion = registry.getVersion();
        perTestCoverage = registryVersion == other.registryVersion ? other.perTestCoverage : spec.getPerTestStrategy().build(registry, spec, DEFAULT_EST_PER_TEST_RECORDINGS);
        empty = other.empty;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isEmpty() {
        return empty;
    }

    /**
     * where possible resolves classnames and method names stored as strings in this data into ClassInfo and MethodInfo
     *  references in the passed registry
     *
     * @param registry the registry to resolve against
     */
    public void resolve(Clover2Registry registry) {
        for (TestCaseInfo info : perTestCoverage.getTests()) {
            info.resolve(registry.getProject());
        }
    }

    /** Required for merge - otherwise this int[] is an internal implementation detail */
    public int [] getHitCounts() {
        return hitCounts;
    }

    @Override
    public int getHitCount(int index) {
        if (index < hitCounts.length) {
            return hitCounts[index];
        } else {
            return 0;
        }
    }

    @Override
    public int getCoverageSize() {
        return hitCounts.length;
    }

    /**
      * @return a bit set for hits attributed to passed tests and incidental coverage (not attributed to tests)
     */
    public BitSet getPassOnlyAndIncidentalHits() {
        BitSet passOnlyTestHits = perTestCoverage.getPassOnlyHits();
        BitSet allTestHits = perTestCoverage.getAllHits();
        BitSet allHits = CloverBitSet.fromIntArray(hitCounts);

        allHits.andNot(allTestHits); // gets just incidental coverage
        passOnlyTestHits.or(allHits);  // adds passed test coverage to incidental coverage

        return passOnlyTestHits;
    }

    @Override
    public TestCaseInfo getTestById(int testId) {
        return perTestCoverage.getTestById(testId);
    }

    @Override
    public BitSet getPassOnlyHits() {
        return perTestCoverage.getPassOnlyHits();
    }

    @Override
    public BitSet getHitsFor(TestCaseInfo tci) {
        return perTestCoverage.getHitsFor(tci);
    }

    @Override
    public BitSet getHitsFor(Set<TestCaseInfo> tcis) {
        return perTestCoverage.getHitsFor(tcis);
    }

    @Override
    public BitSet getHitsFor(Set<TestCaseInfo> tcis, CoverageDataRange range) {
        return perTestCoverage.getHitsFor(tcis, range);
    }

    @Override
    public BitSet getUniqueHitsFor(TestCaseInfo tci) {
        return perTestCoverage.getUniqueHitsFor(tci);
    }

    @Override
    public BitSet getUniqueHitsFor(Set<TestCaseInfo> tcis) {
        return perTestCoverage.getUniqueHitsFor(tcis);
    }

    @Override
    public boolean hasPerTestData() {
        return perTestCoverage.hasPerTestData();
    }

    @Override
    public Set<TestCaseInfo> getTests() {
        return perTestCoverage.getTests();
    }

    @Override
    public Set<TestCaseInfo> getTestsCovering(CoverageDataRange range) {
        return perTestCoverage.getTestsCovering(range);
    }

    @Override
    public Map<TestCaseInfo, BitSet> mapTestsAndCoverageForFile(FullFileInfo fileInfo) {
        return perTestCoverage.mapTestsAndCoverageForFile(fileInfo);
    }

    @Override
    public BitSet getAllHits() {
        return perTestCoverage.getAllHits();
    }

    @Override
    public void addCoverage(TestCaseInfo tci, PerTestRecordingTranscript recording) {
        perTestCoverage.addCoverage(tci, recording);
    }

    @Override
    public void addCoverage(GlobalCoverageRecordingTranscript recording) {
        int added = recording.addTo(hitCounts);
        if (added != recording.getCount()) {
            Logger.getInstance().verbose("Truncated recording file before adding to global coverage: " + recording);
        }
        empty = false;
    }

    public static Set<TestCaseInfo> tcisInHitRange(Map<TestCaseInfo, BitSet> tcisAndHits, CoverageDataRange range) {
        Set<TestCaseInfo> hits = newHashSet();
        for (final Map.Entry<TestCaseInfo, BitSet> tciAndHits : tcisAndHits.entrySet()) {
            final int startIdx = range.getDataIndex();
            final int endIdx = range.getDataIndex() + range.getDataLength();
            final int hitIdx = tciAndHits.getValue().nextSetBit(startIdx);
            if (hitIdx != -1 && hitIdx < endIdx) {
                hits.add(tciAndHits.getKey());
            }
        }
        return hits;
    }

    /**
     * Any negative coverage is an obvious sign of coverage overflow. This method ensures such coverage is
     * set to Integer.MAX_VALUE.
     **/
    public void avoidObviousOverflow() {
        final int max = Integer.MAX_VALUE;
        for (int i = 0; i < hitCounts.length; i++) {
            if (hitCounts[i] < 0) {
                hitCounts[i] = max;
            }
        }
    }

    public PerTestCoverage getPerTestCoverage() {
        return perTestCoverage;
    }
}
