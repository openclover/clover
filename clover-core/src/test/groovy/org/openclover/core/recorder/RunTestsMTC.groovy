package org.openclover.core.recorder

import com.atlassian.clover.recorder.GlobalCoverageRecordingTranscript
import com.atlassian.clover.recorder.RecordingTranscripts;
import org.openclover.runtime.util.CloverBitSet;
import edu.umd.cs.mtc.MultithreadedTestCase

/**
 * Base class for multi-threaded test cases
 */
public abstract class RunTestsMTC extends MultithreadedTestCase {

    /** How accurate our assertions shall be */
    public static enum CoverageAssertionAccuracy {
        EXACT,   // check for exact true/false values in a bitmask
        MIN_MAX  // check using minimum and maximum boundary (subset and superset of a bitmask)
    }

    /**
     * Perform logical or on each i-th argument from input arrays
     * @param args arrays to be or-ed
     * @return boolean[] output
     */
    public static boolean[] or(boolean[] ... args) {
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

    public abstract void assertPerTestHitCounts(final int testCaseNumber, final CloverBitSet perTestCoverage,
                                                CoverageAssertionAccuracy accuracy);
    public abstract void assertGlobalHitCounts(GlobalCoverageRecordingTranscript globalCoverage);
    public abstract void assertGlobalAndPerTestRecordings(RecordingTranscripts.Filter filter) throws IOException;
    public abstract void assertGlobalAndPerTestRecordingsInRange(RecordingTranscripts.Filter filter) throws IOException;
}
