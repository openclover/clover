package org.openclover.core.recorder

import com.atlassian.clover.recorder.GlobalCoverageRecordingTranscript
import org.openclover.runtime.util.CloverBitSet
import com.atlassian.clover.registry.CoverageDataProvider

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * Helper assert methods.
 */
class RecorderTestUtil {

    static void assertGlobalHitCounts(String messagePrefix, final boolean[] expectedHitMask, final GlobalCoverageRecordingTranscript globalCoverage) {
        for (int i = 0; i < expectedHitMask.length; i++) {
            assertTrue(messagePrefix + "Expected hit[$i]" + (expectedHitMask[i] ? ">0" : "=0")
                    + " but found globalCoverage[$i]=" + globalCoverage.get(i) as String,
                    expectedHitMask[i] == (globalCoverage.get(i) > 0))
        }
    }

    static void assertGlobalHitCounts(final boolean[] expectedHitMask, final CoverageDataProvider globalCoverage) {
        for (int i = 0; i < expectedHitMask.length; i++) {
            assertTrue("Expected hit[$i]" + (expectedHitMask[i] ? ">0" : "=0")
                    + " but found globalCoverage[$i]=" + globalCoverage.getHitCount(i) as String,
                    expectedHitMask[i] == (globalCoverage.getHitCount(i) > 0))
        }
    }

    static void assertPerTestHitCounts(String messagePrefix, final boolean[] expectedHitMask, final CloverBitSet perTestCoverage) {
        for (int i = 0; i < expectedHitMask.length; i++) {
            assertEquals(
                    "${messagePrefix}Expected hit[$i]=${expectedHitMask[i]} but found perTestCoverage[$i]=${perTestCoverage.member(i)}".toString(),
                    expectedHitMask[i], perTestCoverage.member(i))
        }
    }

    /**
     * Check if <code>perTestCoverage</code> has at least these bits set as in <code>minimumHitMask</code>
     * (i.e. it can have more bits set, but no bit missing from the mask).
     *
     * @param messagePrefix extra message
     * @param minimumHitMask expected bits
     * @param perTestCoverage value to be check
     */
    static void assertPerTestHitCountsAtLeast(String messagePrefix, boolean[] minimumHitMask, CloverBitSet perTestCoverage) {
        for (int i = 0; i < minimumHitMask.length; i++) {
            if (minimumHitMask[i] && !perTestCoverage.member(i)) {
                fail(messagePrefix + "Expected hit[$i]=" + minimumHitMask[i]
                        + " but found perTestCoverage[$i]=" + perTestCoverage.member(i) as String)
            }
        }
    }

    /**
     * Check if <code>perTestCoverage</code> has at most these bits set as in <code>maximumHitMask</code>
     * (i.e. it can have less bits set to true, but no bit outside the mask).
     * @param messagePrefix
     * @param maximumHitMask
     * @param perTestCoverage
     */
    static void assertPerTestHitCountsAtMost(String messagePrefix, boolean[] maximumHitMask, CloverBitSet perTestCoverage) {
        for (int i = 0; i < maximumHitMask.length; i++) {
            if (perTestCoverage.member(i) && !maximumHitMask[i]) {
                fail(messagePrefix + "Expected hit[$i]=${maximumHitMask[i]}"
                        + " but found perTestCoverage[$i]=" + perTestCoverage.member(i) as String)
            }
        }
    }
}
