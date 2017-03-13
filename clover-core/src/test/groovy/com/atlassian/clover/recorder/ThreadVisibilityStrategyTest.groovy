package com.atlassian.clover.recorder;

import com_atlassian_clover.CloverProfile;
import com_atlassian_clover.CoverageRecorder;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Very basic tests for different thread visibility strategies:
 * <ul>
 *   <li>{@link ThreadVisibilityStrategy.Volatile}</li>
 *   <li>{@link ThreadVisibilityStrategy.Synchronized}</li>
 *   <li>{@link ThreadVisibilityStrategy.SingleThreaded}</li>
 * </ul>
 * using different coverage recorders:
 * <ul>
 *   <li>{@link FixedSizeCoverageRecorder}</li>
 *   <li>{@link GrowableCoverageRecorder}</li>
 *   <li>{@link SharedCoverageRecorder}</li>
 * </ul>
 */
public class ThreadVisibilityStrategyTest {

    @Test
    public void testVolatileHolder_FixedRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithVolatile(CloverProfile.CoverageRecorderType.FIXED);
        assertTrue(recording.get(0));
    }

    @Test
    public void testVolatileHolder_GrowableRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithVolatile(CloverProfile.CoverageRecorderType.GROWABLE);
        assertTrue(recording.get(0));
    }

    @Test
    public void testVolatileHolder_SharedRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithVolatile(CloverProfile.CoverageRecorderType.SHARED);
        assertTrue(recording.get(0));
    }

    @Test
    public void testSynchronizedHolder_FixedRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithSynchronized(CloverProfile.CoverageRecorderType.FIXED);
        assertTrue(recording.get(0));
    }

    @Test
    public void testSynchronizedHolder_GrowableRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithSynchronized(CloverProfile.CoverageRecorderType.GROWABLE);
        assertTrue(recording.get(0));
    }

    @Test
    public void testSynchronizedHolder_SharedRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithSynchronized(CloverProfile.CoverageRecorderType.SHARED);
        assertTrue(recording.get(0));
    }

    @Test
    public void testSingleThreadedHolder_FixedRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithSingleThreaded(CloverProfile.CoverageRecorderType.FIXED);
        assertTrue(recording.get(0));
    }

    @Test
    public void testSingleThreadedHolder_GrowableRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithSingleThreaded(CloverProfile.CoverageRecorderType.GROWABLE);
        assertTrue(recording.get(0));
    }

    @Test
    public void testSingleThreadedHolder_SharedRecorder_BasicallyWorks() {
        final FileBasedPerTestRecording recording = runSliceWithSingleThreaded(CloverProfile.CoverageRecorderType.SHARED);
        assertTrue(recording.get(0));
    }

    @Test
    public void testGrowableRecorder_GrowthBasicallyWorks() {
        final int hitIndex = GrowableCoverageRecorder.CoverageMatrix.WIDTH * 3; // hit outside initial size

        final FileBasedPerTestRecording recordingVolatile =
                runSliceWithVolatile(CloverProfile.CoverageRecorderType.GROWABLE, hitIndex);
        assertFalse(recordingVolatile.get(0));
        assertTrue(recordingVolatile.get(hitIndex));

        final FileBasedPerTestRecording recordingSynchronized =
                runSliceWithSynchronized(CloverProfile.CoverageRecorderType.GROWABLE, hitIndex);
        assertFalse(recordingSynchronized.get(0));
        assertTrue(recordingSynchronized.get(hitIndex));

        final FileBasedPerTestRecording recordingSingle =
                runSliceWithSingleThreaded(CloverProfile.CoverageRecorderType.GROWABLE, hitIndex);
        assertFalse(recordingSingle.get(0));
        assertTrue(recordingSingle.get(hitIndex));
    }

    @Test
    public void testSharedRecorder_GrowthBasicallyWorks() {
        final int hitIndex = GrowableCoverageRecorder.CoverageMatrix.WIDTH * 3; // hit outside initial size

        final FileBasedPerTestRecording recordingVolatile =
                runSliceWithVolatile(CloverProfile.CoverageRecorderType.SHARED, hitIndex);
        assertFalse(recordingVolatile.get(0));
        assertTrue(recordingVolatile.get(hitIndex));

        final FileBasedPerTestRecording recordingSynchronized =
                runSliceWithSynchronized(CloverProfile.CoverageRecorderType.SHARED, hitIndex);
        assertFalse(recordingSynchronized.get(0));
        assertTrue(recordingSynchronized.get(hitIndex));

        final FileBasedPerTestRecording recordingSingle =
                runSliceWithSingleThreaded(CloverProfile.CoverageRecorderType.SHARED, hitIndex);
        assertFalse(recordingSingle.get(0));
        assertTrue(recordingSingle.get(hitIndex));
    }

    /**
     * Execute one test slice with a Volatile thread strategy. Hit index 0.
     * @param type recorder type
     * @return FileBasedPerTestRecording
     */
    protected FileBasedPerTestRecording runSliceWithVolatile(CloverProfile.CoverageRecorderType type) {
        return runSliceWithVolatile(type, 0);
    }

    /**
     * Execute one test slice with a Volatile thread strategy.
     * @param type recorder type
     * @param hitIndex coverage array index to be incremented
     * @return FileBasedPerTestRecording
     */
    protected FileBasedPerTestRecording runSliceWithVolatile(CloverProfile.CoverageRecorderType type, int hitIndex) {
        final ThreadVisibilityStrategy holder = new ThreadVisibilityStrategy.Volatile(newCoverageRecorder(type));
        holder.testStarted(getClass().getName(), System.currentTimeMillis(), 0, 0);
        holder.set(hitIndex);
        return (FileBasedPerTestRecording)holder.testFinished(getClass().getName(), "test", "runtimeTestName",
                System.currentTimeMillis(), 0, 0, 1, null);
    }

    /**
     * Execute one test slice with a Synchronized thread strategy. Hit index 0.
     * @param type recorder type
     * @return FileBasedPerTestRecording
     */
    protected FileBasedPerTestRecording runSliceWithSynchronized(CloverProfile.CoverageRecorderType type) {
        return runSliceWithSynchronized(type, 0);
    }

    /**
     * Execute one test slice with a Synchronized thread strategy.
     * @param type recorder type
     * @param hitIndex coverage array index to be incremented
     * @return FileBasedPerTestRecording
     */
    protected FileBasedPerTestRecording runSliceWithSynchronized(CloverProfile.CoverageRecorderType type, int hitIndex) {
        final ThreadVisibilityStrategy holder = new ThreadVisibilityStrategy.Synchronized(newCoverageRecorder(type));
        holder.testStarted(getClass().getName(), System.currentTimeMillis(), 0, 0);
        holder.set(hitIndex);
        return (FileBasedPerTestRecording)holder.testFinished(getClass().getName(), "test", null,
                System.currentTimeMillis(), 0, 0, 1, null);
    }

    /**
     * Execute one test slice with a SingleThreaded thread strategy. Hit index 0.
     * @param type recorder type
     * @return FileBasedPerTestRecording
     */
    protected FileBasedPerTestRecording runSliceWithSingleThreaded(CloverProfile.CoverageRecorderType type) {
        return runSliceWithSingleThreaded(type, 0);
    }

    /**
     * Execute one test slice with a SingleThreaded thread strategy.
     * @param type recorder type
     * @param hitIndex coverage array index to be incremented
     * @return FileBasedPerTestRecording
     */
    protected FileBasedPerTestRecording runSliceWithSingleThreaded(CloverProfile.CoverageRecorderType type, int hitIndex) {
        final ThreadVisibilityStrategy holder = new ThreadVisibilityStrategy.SingleThreaded(newCoverageRecorder(type));
        holder.testStarted(getClass().getName(), System.currentTimeMillis(), 0, 0);
        holder.set(hitIndex);
        return (FileBasedPerTestRecording)holder.testFinished(getClass().getName(), "test", null,
                System.currentTimeMillis(), 0, 0, 1, null);
    }

    /**
     * Create new coverage recorder instance
     * @param type recorder type
     * @return CoverageRecorder
     */
    private static CoverageRecorder newCoverageRecorder(CloverProfile.CoverageRecorderType type) {
        final String dbName = "clover.db";
        final long dbVersion = 0;
        final int maxElements = 100;
        final long cfgBits = 0;
        switch (type) {
            case CloverProfile.CoverageRecorderType.FIXED:
                return new FixedSizeCoverageRecorder(dbName, dbVersion, maxElements, cfgBits);
            case CloverProfile.CoverageRecorderType.GROWABLE:
                return new GrowableCoverageRecorder(dbName, dbVersion, cfgBits, maxElements);
            case CloverProfile.CoverageRecorderType.SHARED:
                return SharedCoverageRecorder.createFor(dbName, dbVersion, cfgBits, maxElements);
        }
        return null;
    }
}
