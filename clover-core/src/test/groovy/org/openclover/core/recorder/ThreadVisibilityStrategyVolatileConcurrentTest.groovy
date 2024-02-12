package org.openclover.core.recorder

import org_openclover_runtime.CloverProfile
import org_openclover_runtime.CoverageRecorder
import org.junit.Ignore
import org.junit.Test

/**
 * Concurrent test for {@link org.openclover.runtime.recorder.ThreadVisibilityStrategy.Volatile}
 * against different coverage recorders:
 * <ul>
 *   <li>{@link org.openclover.runtime.recorder.FixedSizeCoverageRecorder}</li>
 *   <li>{@link org.openclover.runtime.recorder.GrowableCoverageRecorder}</li>
 *   <li>{@link org.openclover.runtime.recorder.SharedCoverageRecorder}</li>
 * </ul>
 */
@Ignore // TODO enable this test when CloverBitSet#set() is tread safe (CLOV-1253)
class ThreadVisibilityStrategyVolatileConcurrentTest extends ThreadVisibilityStrategyConcurrentTestBase {

    public static final String VOLATILE_STRATEGY = "volatile"

    @Test
    void testVolatile_FixedRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "volatile_fixed_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.FIXED, VOLATILE_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testVolatile_GrowableRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "volatile_growable_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.GROWABLE, VOLATILE_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testVolatile_SharedRecorder_InOrder() throws Throwable {
        // prepare recorder
        final String dbName = "volatile_shared_inorder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.SHARED, VOLATILE_STRATEGY)

        // run tests
        RunTestsInOrderMTC mtc = runInOrderTestCompletion(recorder)

        // verify results
        assertInOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testVolatile_FixedRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "volatile_fixed_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.FIXED, VOLATILE_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testVolatile_GrowableRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "volatile_growable_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.GROWABLE, VOLATILE_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

    @Test
    void testVolatile_SharedRecorder_OutOfOrder() throws Throwable {
        // prepare recorder
        final String dbName = "volatile_shared_outoforder.db"
        final File cloverDb = new File(tempDir, dbName)
        final CoverageRecorder recorder = newCoverageRecorder(
                cloverDb, CloverProfile.CoverageRecorderType.SHARED, VOLATILE_STRATEGY)

        // run tests
        RunTestsOutOfOrderMTC mtc = runOutOfOrderTestCompletion(recorder)

        // verify results
        assertOutOfOrderTestCompletion(mtc, createFilterForDb(tempDir, dbName))
    }

}
