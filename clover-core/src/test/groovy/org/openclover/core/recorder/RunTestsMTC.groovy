package org.openclover.core.recorder

import edu.umd.cs.mtc.MultithreadedTestCase
import org.openclover.runtime.util.CloverBitSet

/**
 * Base class for multi-threaded test cases
 */
abstract class RunTestsMTC extends MultithreadedTestCase {

    /** How accurate our assertions shall be */
    static enum CoverageAssertionAccuracy {
        EXACT,   // check for exact true/false values in a bitmask
        MIN_MAX  // check using minimum and maximum boundary (subset and superset of a bitmask)
    }

    /**
     * Perform logical or on each i-th argument from input arrays
     * @param args arrays to be or-ed
     * @return boolean[] output
     */
    static boolean[] or(boolean[] ... args) {
        // find the longest input array
        int maxLength = 0;
        for (boolean[] arg : args) {
            maxLength = Math.max(maxLength, arg.length);
        }

        // allocate result
        final boolean[] c = new boolean[maxLength];

        // walk through i-th index of each array and calc or
        for (int i = 0; i < maxLength; i++) {
            boolean or = false;
            for (boolean[] arg : args) {
                if (i < arg.length)
                    or |= arg[i];
            }
            c[i] = or;
        }
        return c;
    }

    abstract void assertPerTestHitCounts(final int testCaseNumber, final CloverBitSet perTestCoverage,
                                         CoverageAssertionAccuracy accuracy);

    abstract void assertGlobalHitCounts(GlobalCoverageRecordingTranscript globalCoverage);

    abstract void assertGlobalAndPerTestRecordings(RecordingTranscripts.Filter filter) throws IOException;

    abstract void assertGlobalAndPerTestRecordingsInRange(RecordingTranscripts.Filter filter) throws IOException;
}
