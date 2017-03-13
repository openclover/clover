package com.atlassian.clover.recorder

import com_atlassian_clover.CloverProfile
import com_atlassian_clover.CoverageRecorder
import org.junit.Test

/**
 * Concurrent test for {@link com.atlassian.clover.recorder.ThreadVisibilityStrategy.Synchronized}
 * against different coverage recorders:
 * <ul>
 *   <li>{@link com.atlassian.clover.recorder.FixedSizeCoverageRecorder}</li>
 *   <li>{@link com.atlassian.clover.recorder.GrowableCoverageRecorder}</li>
 *   <li>{@link com.atlassian.clover.recorder.SharedCoverageRecorder}</li>
 * </ul>
 */
class ThreadVisibilityStrategySynchronizedConcurrentTest extends ThreadVisibilityStrategyConcurrentTestBase {

    public static final String SYNCHRONIZED_STRATEGY = "synchronized"

    @Test
    void testSynchronized_FixedRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "synch_fixed_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.FIXED, SYNCHRONIZED_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSynchronized_GrowableRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "synch_growable_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.GROWABLE, SYNCHRONIZED_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSynchronized_SharedRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "synch_shared_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.SHARED, SYNCHRONIZED_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSynchronized_FixedRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "synch_fixed_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.FIXED, SYNCHRONIZED_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSynchronized_GrowableRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "synch_growable_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.GROWABLE, SYNCHRONIZED_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSynchronized_SharedRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "synch_shared_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.SHARED, SYNCHRONIZED_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

}
