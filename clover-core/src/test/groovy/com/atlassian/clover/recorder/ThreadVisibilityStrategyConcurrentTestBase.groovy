package com.atlassian.clover.recorder

import org.openclover.runtime.CloverNames
import com.atlassian.clover.TestUtils
import org_openclover_runtime.CloverProfile
import org_openclover_runtime.CoverageRecorder
import edu.umd.cs.mtc.TestFramework
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.openclover.runtime.recorder.GrowableCoverageRecorder
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder
import org.openclover.runtime.recorder.SharedCoverageRecorder

import static org.junit.Assert.assertEquals

/**
 * Common stuff for true concurrent testing of different thread visibility strategies:
 * <ul>
 *   <li>{@link com.atlassian.clover.recorder.ThreadVisibilityStrategy.Volatile}</li>
 *   <li>{@link com.atlassian.clover.recorder.ThreadVisibilityStrategy.Synchronized}</li>
 *   <li>{@link com.atlassian.clover.recorder.ThreadVisibilityStrategy.SingleThreaded}</li>
 * </ul>
 * against different coverage recorders:
 * <ul>
 *   <li>{@link FixedSizeCoverageRecorder}</li>
 *   <li>{@link GrowableCoverageRecorder}</li>
 *   <li>{@link SharedCoverageRecorder}</li>
 * </ul>
 */
abstract class ThreadVisibilityStrategyConcurrentTestBase {

    @Rule
    public TestName name = new TestName()

    protected File tempDir

    private static final int TIMEOUT_60_SEC = 60

    @Before
    void setUp() throws IOException {
        tempDir = TestUtils.createEmptyDirFor(getClass(), name.getMethodName())
    }

    RunTestsInOrderMTC runInOrderTestCompletion(final CoverageRecorder coverageRecorder) throws Throwable {
        RunTestsInOrderMTC mtc = new RunTestsInOrderMTC(coverageRecorder)
        TestFramework.runOnce(mtc, null, TIMEOUT_60_SEC)
        coverageRecorder.flush()
        return mtc
    }

    RunTestsOutOfOrderMTC runOutOfOrderTestCompletion(final CoverageRecorder coverageRecorder) throws Throwable {
        RunTestsOutOfOrderMTC mtc = new RunTestsOutOfOrderMTC(coverageRecorder)
        TestFramework.runOnce(mtc, null, TIMEOUT_60_SEC)
        coverageRecorder.flush()
        return mtc
    }


    protected void assertInOrderTestCompletion(RunTestsInOrderMTC mtc, RecordingTranscripts.Filter filterForDb) throws IOException {
        mtc.assertGlobalAndPerTestRecordings(filterForDb)
    }

    protected void assertInOrderTestCompletionInRange(RunTestsInOrderMTC mtc, RecordingTranscripts.Filter filterForDb) throws IOException {
        mtc.assertGlobalAndPerTestRecordingsInRange(filterForDb)
    }

    protected void assertOutOfOrderTestCompletion(RunTestsOutOfOrderMTC mtc, RecordingTranscripts.Filter filterForDb) throws IOException {
        mtc.assertGlobalAndPerTestRecordings(filterForDb)
    }

    protected void assertOutOfOrderTestCompletionInRange(RunTestsOutOfOrderMTC mtc, RecordingTranscripts.Filter filterForDb) throws IOException {
        mtc.assertGlobalAndPerTestRecordingsInRange(filterForDb)
    }

    /**
     * Creates new coverage recorder instance
     *
     * @param cloverDb path to clover database (does not have to exist, it will be used as a location for coverage files)
     * @param type global recorder type
     * @param threadStrategy strategy for multi-threaded per-test recording
     * @return CoverageRecorder
     */
    protected CoverageRecorder newCoverageRecorder(final File cloverDb,
                                                   final CloverProfile.CoverageRecorderType type,
                                                   final String threadStrategy) throws IOException {
        final long dbVersion = 0
        final int maxElements = 100
        final long cfgBits = 0

        // set visibility strategy, check if value was set
        System.setProperty(CloverNames.PROP_PER_TEST_COV_THREADING, threadStrategy)
        assertEquals(threadStrategy, System.getProperty(CloverNames.PROP_PER_TEST_COV_THREADING))

        // return proper recorder type (with a per-test thread strategy)
        switch (type) {
            case CloverProfile.CoverageRecorderType.FIXED:
                return new FixedSizeCoverageRecorder(cloverDb.getAbsolutePath(), dbVersion, maxElements, cfgBits)
            case CloverProfile.CoverageRecorderType.GROWABLE:
                return GrowableCoverageRecorder.createFor(cloverDb.getAbsolutePath(), dbVersion, cfgBits, maxElements)
            case CloverProfile.CoverageRecorderType.SHARED:
                return SharedCoverageRecorder.createFor(cloverDb.getAbsolutePath(), dbVersion, cfgBits, maxElements)
        }

        return NullRecorder.INSTANCE
    }

    /**
     * Creates a filter which finds global and per-test coverage files for given database.
     *
     * @param dir    directory containing database and coverage files
     * @param dbName database file name (base name)
     * @return RecordingTranscripts.Filter
     */
    protected RecordingTranscripts.Filter createFilterForDb(File dir, String dbName) {
        RecordingTranscripts.Filter filter = new RecordingTranscripts.Filter(dir, dbName,
                Long.MIN_VALUE, Long.MAX_VALUE, false, true)
        filter.collectAllFiles()
        return filter
    }
}
