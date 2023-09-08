package com.atlassian.clover.recorder;

import com.atlassian.clover.registry.entities.TestCaseInfo;

import java.util.BitSet;
import java.util.Set;
import java.io.Serializable;

import static org.openclover.util.Sets.newHashSet;

public abstract class BasePerTestCoverage implements PerTestCoverage, Serializable {
    private static final long serialVersionUID = 8596722259646445122L;

    protected final int coverageSize;

    /** Mask used to calculate unique coverage for a SINGLE TCI only */
    protected transient BitSet uniqueCoverageMask;

    public BasePerTestCoverage(int coverageSize) {
        this.coverageSize = coverageSize;
    }

    @Override
    public int getCoverageSize() {
        return coverageSize;
    }

    protected BitSet getUniqueCoverageMask() {
        if (uniqueCoverageMask == null) {
            initMasks();
        }
        return uniqueCoverageMask;
    }

    protected abstract void initMasks();

    @Override
    public BitSet getUniqueHitsFor(TestCaseInfo tci) {
        BitSet hits = getHitsFor(tci);
        hits = (BitSet)hits.clone();
        hits.and(getUniqueCoverageMask());
        return hits;
    }

    @Override
    public BitSet getUniqueHitsFor(Set<TestCaseInfo> slices) {
        if (slices.size() == 1) {
            return getUniqueHitsFor(slices.iterator().next());
        } else {
            //Coverage for set of TCIs
            BitSet coverage = getHitsFor(slices, null);

            Set<TestCaseInfo> otherSlices = newHashSet(getTests());
            otherSlices.removeAll(slices);
            //Coverage for TCIs not in set
            BitSet otherCoverage = getHitsFor(otherSlices, null);

            coverage.andNot(otherCoverage);
            return coverage;
        }
    }
}
