package org.openclover.core.recorder

import org.openclover.runtime.RuntimeType
import org_openclover_runtime.CoverageRecorder
import org.junit.Test
import org.openclover.runtime.recorder.ActivePerTestRecorderOne
import org.openclover.runtime.recorder.GrowableCoverageRecorder

import static org.junit.Assert.*

/**
 * Test for {@link com.atlassian.clover.recorder.ActivePerTestRecorderOne}
 */
class PerTestRecorderOneTest {

    @Test
    void testOneWithGrowableCoverageRecorder() {
        // initial size - 10 elements
        final CoverageRecorder recorder = new GrowableCoverageRecorder("clover.db", 0, 0, 10)
        final ActivePerTestRecorderOne testRecorder = new ActivePerTestRecorderOne(recorder, recorder.createEmptyHitsMask(),
                new RuntimeType("abc"), 0,0, 0)

        // grow above initial CoverageMatrix size
        int matrixWidth = GrowableCoverageRecorder.CoverageMatrix.WIDTH
        for (int i = 0; i < matrixWidth; i++) {
            testRecorder.set(i * 10)
        }

        // check size of the bitset and shortcut array
        assertTrue(testRecorder.coverage.size() >= matrixWidth * 10)
        assertTrue(testRecorder.coverageShortcut.length == matrixWidth * 16); // we're always at least doubling the size

        // check if some bits are set as expected
        assertTrue(testRecorder.coverage.member(0))
        assertTrue(testRecorder.coverageShortcut[0])

        assertFalse(testRecorder.coverage.member(1))
        assertFalse(testRecorder.coverageShortcut[1])

        assertTrue(testRecorder.coverage.member(10))
        assertTrue(testRecorder.coverageShortcut[10])

        assertFalse(testRecorder.coverage.member(matrixWidth * 10 - 1))
        assertFalse(testRecorder.coverageShortcut[matrixWidth * 10 - 1])

        assertTrue(testRecorder.coverage.member( (matrixWidth-1) * 10))
        assertTrue(testRecorder.coverageShortcut[ (matrixWidth-1) * 10])
    }

}
