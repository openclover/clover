package org.openclover.core.recorder

import org.junit.Test
import org.openclover.runtime.recorder.GrowableCoverageRecorder

import static org.junit.Assert.assertEquals

class CoverageMatrixTest {
    @Test
    void testZeroElements() {
        final GrowableCoverageRecorder.CoverageMatrix coverage = new GrowableCoverageRecorder.CoverageMatrix(0)
        assertEquals(0, coverage.getNumElements())
        assertEquals(1, coverage.getHits().length)
        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH, coverage.getHits()[0].length)
    }

    @Test
    void testWidthElements() {
        final GrowableCoverageRecorder.CoverageMatrix coverage = new GrowableCoverageRecorder.CoverageMatrix(GrowableCoverageRecorder.CoverageMatrix.WIDTH)
        coverage.inc(0)
        coverage.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH - 1)

        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH, coverage.getNumElements())
        assertEquals(1, coverage.getHits().length)
        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH, coverage.getHits()[0].length)
        assertEquals(1, coverage.getHits()[0][0])
        assertEquals(1, coverage.getHits()[0][GrowableCoverageRecorder.CoverageMatrix.WIDTH - 1])
    }

    @Test
    void testExpansion() {
        GrowableCoverageRecorder.CoverageMatrix coverage = new GrowableCoverageRecorder.CoverageMatrix(GrowableCoverageRecorder.CoverageMatrix.WIDTH)
        coverage.inc(0)
        coverage.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH - 1)

        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH, coverage.getNumElements())
        assertEquals(1, coverage.getHits().length)
        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH, coverage.getHits()[0].length)
        assertEquals(1, coverage.getHits()[0][0])
        assertEquals(1, coverage.getHits()[0][GrowableCoverageRecorder.CoverageMatrix.WIDTH - 1])

        coverage = new GrowableCoverageRecorder.CoverageMatrix(coverage, GrowableCoverageRecorder.CoverageMatrix.WIDTH * 2)
        coverage.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH)
        coverage.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH * 2 - 1)

        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH * 2, coverage.getNumElements())
        assertEquals(2, coverage.getHits().length)
        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH, coverage.getHits()[0].length)
        assertEquals(GrowableCoverageRecorder.CoverageMatrix.WIDTH, coverage.getHits()[1].length)
        assertEquals(1, coverage.getHits()[0][0])
        assertEquals(1, coverage.getHits()[1][0])
        assertEquals(1, coverage.getHits()[0][GrowableCoverageRecorder.CoverageMatrix.WIDTH - 1])
        assertEquals(1, coverage.getHits()[1][GrowableCoverageRecorder.CoverageMatrix.WIDTH - 1])
    }
}
