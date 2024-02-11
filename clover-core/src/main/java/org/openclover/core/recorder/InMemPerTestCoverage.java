package org.openclover.core.recorder;

import clover.it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import clover.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.registry.CoverageDataRange;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.TestCaseInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;

/**
 * Models test hits against element slots. Slot count is fixed
 * but test count can grow over time. This class is not threadsafe
 * and not intended to be so. Slots are implemented as
 * BitSets which are very space efficient.
 */
public class InMemPerTestCoverage extends BasePerTestCoverage implements Serializable {
    private static final long serialVersionUID = 0L;

    private final Map<TestCaseInfo,BitSet> tciToHits;

    private transient Int2ObjectMap tciIDToTCIMap;

    public InMemPerTestCoverage(int coverageSize) {
        super(coverageSize);
        this.tciToHits = new LinkedHashMap<>();
        this.tciIDToTCIMap = new Int2ObjectOpenHashMap();
    }

    public InMemPerTestCoverage(Clover2Registry registry) {
        this(registry.getDataLength());
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        rebuildTCIIDMap();
    }

    /**
     * Constructs a new instance taking shallow copies of any fields where
     * appropriate. Widens or truncates the per-test slot BitSets as
     * appropriate.
     */
    public InMemPerTestCoverage(InMemPerTestCoverage other, int coverageSize) {
        super(coverageSize);
        this.tciToHits = new LinkedHashMap<>(other.tciToHits);
        for (Map.Entry<TestCaseInfo, BitSet> entry : tciToHits.entrySet()) {
            BitSet slots = entry.getValue();
            if (slots.size() > coverageSize) {
                //subset
                entry.setValue(slots.get(0, coverageSize));
            } else {
                //superset
                BitSet newSlots = new BitSet(coverageSize);
                newSlots.or(slots);
                entry.setValue(newSlots);
            }
        }
        this.tciIDToTCIMap = new Int2ObjectOpenHashMap(other.tciIDToTCIMap);
    }

    @Override
    public TestCaseInfo getTestById(int id) {
        return (TestCaseInfo)tciIDToTCIMap.get(id);
    }

    @SuppressWarnings("unchecked")
    private void rebuildTCIIDMap() {
        tciIDToTCIMap = new Int2ObjectOpenHashMap();
        for (TestCaseInfo tci : tciToHits.keySet()) {
            tciIDToTCIMap.put(tci.getId(), tci);
        }
    }

    @Override
    public boolean hasPerTestData() {
        return !tciToHits.isEmpty();
    }

    @Override
    public Set<TestCaseInfo> getTests() {
        return new LinkedHashSet<>(tciToHits.keySet());
    }

    @Override
    protected void initMasks() {
        //No aggregate test coverage to begin with
        BitSet coveredMask = new BitSet(coverageSize);
        //All coverage is unique to start with until later proven otherwise
        BitSet coverageNotUniqueMask = new BitSet(coverageSize);

        for (BitSet bs : tciToHits.values()) {
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                boolean alreadyCovered = coveredMask.get(i);
                if (alreadyCovered) {
                    coverageNotUniqueMask.set(i);
                } else {
                    coveredMask.set(i);
                }
            }
        }

        coverageNotUniqueMask.flip(0, coverageNotUniqueMask.size());
        uniqueCoverageMask = coverageNotUniqueMask;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BitSet getHitsFor(TestCaseInfo tci) {
        BitSet hits = tciToHits.get(tci);
        if (hits == null) {
            hits = new BitSet(coverageSize);
            tciToHits.put(tci, hits);
            tciIDToTCIMap.put(tci.getId(), tci);
        }
        return hits;
    }

    @Override
    public BitSet getPassOnlyHits() {
        return getCoverage(TestCaseInfoPredicate.SUCCESS_ONLY, null);
    }

    @Override
    public BitSet getAllHits() {
        return getCoverage(TestCaseInfoPredicate.ALL, null);
    }

    /** @return a non-null slot set with those that intersect the given slices/tcis set to true */
    @Override
    public BitSet getHitsFor(final Set<TestCaseInfo> tcis) {
        return getHitsFor(tcis, null);
    }

    /** @return a non-null slot set with those that intersect the given slices/tcis in the given range set to true */
    @Override
    public BitSet getHitsFor(final Set<TestCaseInfo> tcis, CoverageDataRange range) {
        return getCoverage(tcis::contains, range);
    }

    /** @return a non-null slot set with those that satisfy the predicate set to true */
    private BitSet getCoverage(final TestCaseInfoPredicate predicate, final CoverageDataRange range) {
        BitSet coverage = new BitSet(coverageSize);
        int start = 0;
        int end = 0;
        if (range != null) {
            start = range.getDataIndex();
            end = start + range.getDataLength();
        }
        for (Map.Entry<TestCaseInfo, BitSet> entry : tciToHits.entrySet()) {
            BitSet hitsForSlice = entry.getValue();
            if (predicate.eval(entry.getKey())) {
                if (range != null) {
                    for (int j = hitsForSlice.nextSetBit(start); j >= 0 && j < end; j = hitsForSlice.nextSetBit(j + 1)) {
                        coverage.set(j);
                    }
                } else {
                    coverage.or(hitsForSlice);
                }
            }
        }
        return coverage;
    }

    /** @return an unordered set of tcis that intersect with the given receptors */
    @Override
    public Set<TestCaseInfo> getTestsCovering(CoverageDataRange range) {
        Set<TestCaseInfo> tcis = newHashSet();
        for (Map.Entry<TestCaseInfo, BitSet> entry : tciToHits.entrySet()) {
            TestCaseInfo tci = entry.getKey();
            BitSet hits = entry.getValue();
            int firstPass = hits.nextSetBit(range.getDataIndex());
            if (firstPass > -1 && firstPass < (range.getDataIndex() + range.getDataLength())) {
                tcis.add(tci);
            }
        }
        return tcis;
    }

    @Override
    public Map<TestCaseInfo, BitSet> mapTestsAndCoverageForFile(FullFileInfo fileInfo) {
        Map<TestCaseInfo, BitSet> coverage = newHashMap();
        for (Map.Entry<TestCaseInfo, BitSet> entry : tciToHits.entrySet()) {
            TestCaseInfo tci = entry.getKey();
            BitSet hits = entry.getValue();
            int firstPass = hits.nextSetBit(fileInfo.getDataIndex());
            if (firstPass > -1 && firstPass < (fileInfo.getDataIndex() + fileInfo.getDataLength())) {
                coverage.put(tci, hits);
            }
        }
        return coverage;
    }

    @Override
    public void addCoverage(TestCaseInfo tci, PerTestRecordingTranscript recording) {
        recording.applyTo(getHitsFor(tci));
    }

    /** Predicate for filtering on tcis */
    private interface TestCaseInfoPredicate {
        TestCaseInfoPredicate SUCCESS_ONLY = TestCaseInfo::isSuccess;
        TestCaseInfoPredicate ALL = tci -> true;
        boolean eval(final TestCaseInfo slice);
    }
}
