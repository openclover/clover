package org.openclover.runtime.recorder;

/**
 * Coverage at a point in time - not to be confused with a test optimization snapshot
 */
public final class CoverageSnapshot {
    private final int[][] coverage;

    CoverageSnapshot(int[][] coverage) {
        this.coverage = coverage;
    }

    public int[][] getCoverage() {
        return coverage;
    }
}
