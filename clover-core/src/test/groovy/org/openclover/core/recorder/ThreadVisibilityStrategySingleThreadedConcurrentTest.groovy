package org.openclover.core.recorder

import org.junit.Ignore
import org.junit.Test
import org_openclover_runtime.CloverProfile
import org_openclover_runtime.CoverageRecorder

/**
 * Concurrent test for {@link org.openclover.runtime.recorder.ThreadVisibilityStrategy.SingleThreaded} against different
 * coverage recorders: <ul>
 * <li>{@link org.openclover.runtime.recorder.FixedSizeCoverageRecorder}</li>
 * <li>{@link org.openclover.runtime.recorder.GrowableCoverageRecorder}</li>
 * <li>{@link org.openclover.runtime.recorder.SharedCoverageRecorder}</li>
 * </ul>
 */
@Ignore // TODO enable this test when CloverBitSet#set() is tread safe (CLOV-1253)
class ThreadVisibilityStrategySingleThreadedConcurrentTest extends ThreadVisibilityStrategyConcurrentTestBase {

    public static final String SINGLE_THREADED_STRATEGY = ""

    @Test
    void testSingleThreaded_FixedRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "single_fixed_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.FIXED, SINGLE_THREADED_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results, use weak check as it's for single thread by design
        assertInOrderTestCompletionInRange(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSingleThreaded_GrowableRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "single_growable_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.GROWABLE, SINGLE_THREADED_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletionInRange(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSingleThreaded_SharedRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "single_shared_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.SHARED, SINGLE_THREADED_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletionInRange(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSingleThreaded_FixedRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "single_fixed_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.FIXED, SINGLE_THREADED_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletionInRange(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSingleThreaded_GrowableRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "single_growable_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.GROWABLE, SINGLE_THREADED_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletionInRange(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testSingleThreaded_SharedRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "single_shared_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.SHARED, SINGLE_THREADED_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletionInRange(mtc, createFilterForDb(tempDir, dbName))
    }

}
