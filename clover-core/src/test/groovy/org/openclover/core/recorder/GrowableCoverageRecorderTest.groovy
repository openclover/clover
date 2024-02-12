package org.openclover.core.recorder

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.runtime.recorder.CoverageSnapshot
import org.openclover.runtime.recorder.GlobalRecordingWriteStrategy
import org.openclover.runtime.recorder.GrowableCoverageRecorder
import org.openclover.runtime.util.CloverBitSet
import org_openclover_runtime.CoverageRecorder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class GrowableCoverageRecorderTest {

    @Rule
    public TestName testName = new TestName()
    
    @Test
    void testGrowth() {
        final int[] elementCountRef = new int[1]
        final int[][][] hitsRef = new int[1][][]

        final CoverageRecorder recorder =
            new GrowableCoverageRecorder("foo", 0, 0, GrowableCoverageRecorder.CoverageMatrix.WIDTH , new GlobalRecordingWriteStrategy() {
                String write(String recordingFileName, long dbVersion, long lastFlush, int[][] hits, int elementCount) throws IOException {
                    elementCountRef[0] = elementCount
                    hitsRef[0] = hits
                    return testName.methodName
                }
            }).withCapacityFor(GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        recorder.iget(0)
        recorder.inc(3)
        recorder.inc(7)

        //Should force growth
        final CoverageRecorder proxyRecorder = recorder.withCapacityFor(2 * GrowableCoverageRecorder.CoverageMatrix.WIDTH)
        proxyRecorder.inc(0)
        proxyRecorder.inc(3)
        proxyRecorder.iget(7)
        proxyRecorder.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH)
        proxyRecorder.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 4)
        proxyRecorder.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 8)

        recorder.forceFlush()

        assertEquals(2 * GrowableCoverageRecorder.CoverageMatrix.WIDTH, elementCountRef[0])

        assertEquals(2, elementOf(0, hitsRef[0]))
        assertEquals(2, elementOf(3, hitsRef[0]))
        assertEquals(2, elementOf(7, hitsRef[0]))
        assertEquals(1, elementOf(GrowableCoverageRecorder.CoverageMatrix.WIDTH, hitsRef[0]))
        assertEquals(1, elementOf(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 4, hitsRef[0]))
        assertEquals(1, elementOf(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 8, hitsRef[0]))
    }

    @Test
    void testNoGrowth() {
        final CoverageRecorder recorder = GrowableCoverageRecorder.createFor("foo", 0, 0, GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        recorder.inc(0)
        recorder.inc(3)
        recorder.iget(7)

        assertSame(recorder, recorder.withCapacityFor((GrowableCoverageRecorder.CoverageMatrix.WIDTH / 2) as int))
    }

    @Test
    void testCoverageComparisonWithGrowth() {
        CoverageRecorder recorder = GrowableCoverageRecorder.createFor("foo", 0, 0, GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        final CoverageSnapshot before = recorder.getCoverageSnapshot()

        recorder.inc(0)
        recorder.inc(3)
        recorder.iget(7)

        final CloverBitSet mask = recorder.compareCoverageWith(before)
        assertTrue(mask.member(0))
        assertTrue(mask.member(3))
        assertTrue(mask.member(7))

        assertTrue(mask.size() >= GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        recorder = recorder.withCapacityFor(2 * GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        recorder.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 0)
        recorder.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 4)
        recorder.iget(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 8)

        final CloverBitSet mask2 = recorder.compareCoverageWith(before)
        assertFalse(mask == mask2)

        assertTrue(mask2.member(0))
        assertTrue(mask2.member(3))
        assertTrue(mask2.member(7))

        assertTrue(mask2.member(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 0))
        assertTrue(mask2.member(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 4))
        assertTrue(mask2.member(GrowableCoverageRecorder.CoverageMatrix.WIDTH + 8))
    }

    @Test
    void testEmptyMaskWithGrowth() {
        CoverageRecorder recorder = GrowableCoverageRecorder.createFor("foo", 0, 0, GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        final CloverBitSet mask = recorder.createEmptyHitsMask()
        assertTrue(mask.size() >= GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        recorder = recorder.withCapacityFor(2 * GrowableCoverageRecorder.CoverageMatrix.WIDTH)

        final CloverBitSet mask2 = recorder.createEmptyHitsMask()
        assertTrue(mask2.size() >= 2 * GrowableCoverageRecorder.CoverageMatrix.WIDTH)
        assertFalse(mask.is(mask2))
    }

    @Test
    void testAIOOBEIfProxyNotGrown() {
        final CoverageRecorder recorder = new GrowableCoverageRecorder.FixedProxy(new GrowableCoverageRecorder("foo", 0, 0, GrowableCoverageRecorder.CoverageMatrix.WIDTH))

        try {
            recorder.inc(GrowableCoverageRecorder.CoverageMatrix.WIDTH)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (ArrayIndexOutOfBoundsException e) {
            //pass
        }
    }

    @Test
    void testCantIGetOrInc() {
        final CoverageRecorder recorder = new GrowableCoverageRecorder("foo", 0, 0, GrowableCoverageRecorder.CoverageMatrix.WIDTH)
        try {
            recorder.iget(0)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (UnsupportedOperationException e) {
            //pass
        }

        try {
            recorder.inc(0)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (Exception e) {
            //pass
        }
    }

    private int elementOf(int index, int[][] hits) {
        final int depth = hits.length
        final int width = hits[0].length
        return hits[index / width][index % width]
    }
}
