package org.openclover.core.recorder

import org.openclover.core.CoverageDataSpec
import org.openclover.runtime.util.CloverBitSet
import org_openclover_runtime.CoverageRecorder

/**
 * Test case which runs three threads. Each thread runs a single test case (i.e. test start/end detected by OpenClover).
 * These test cases start and end in different order. Code coverage from test cases is as follows:
 * <pre>
 *     thread1 thread2 thread3
 *     TestA   TestB   TestC      Global
 *  0    x       x      (x)          x      //time=3
 *  1            x      (x)          x
 *  2    x              (x)          x
 *  3                   (x)          x
 *  4    x       x                   x
 *  5            x                   x
 *  6    x                           x
 *  7
 *  8    x                           x      //time=6
 *  9
 *  10                   x           x
 *  11                   x           x       //time=8
 * </pre>
 *
 * Order in which test case starts (s) and ends (e) and code coverage is written (*):
 * <pre>
 *    time    0   1   2   3   4   5   6   7   8   9
 *    TestA       s ------*-----------*-- e
 *    TestB           s --*-- e
 *    TestC                       s --*-------*-- e
 * </pre>
 *
 * so we check:
 * <ul>
 *   <li>TestB inside TestA</li>
 *   <li>TestC overlaps TestA</li>
 *   <li>TestB independent from TestC</li>
 * </ul>
 *
 * Expected result: each test case gets coverage generated from all running threads at time when the test was active
 * as a result the code coverage reported can be higher than from a single test cases's thread.
 *
 * <pre>
 *    TestA will get hits from time=3+6 = minimum A, maximum A+B+C'
 *    TestB will get hits from time=3   = minimum B, maximum A'+B
 *    TestC will get hits from time=6+8 = minimum C, maximum A"+C
 * </pre>
 */
class RunTestsOutOfOrderMTC extends RunTestsMTC {

    /** Expected values */
    public static boolean[] TEST_A3_HITS = [
            true, false, true, false, true,
            false, true, false, false, false,
            false, false ];
    public static boolean[] TEST_A6_HITS = [
            false, false, false, false, false,
            false, false, false, true, false,
            false, false ];
    public static boolean[] TEST_B_HITS = [
            true, true, false, false, true,
            true, false, false, false, false,
            false, false ];
    public static boolean[] TEST_C6_HITS = [
            true, true, true, true, false,
            false, false, false, false, false,
            true, false ];
    public static boolean[] TEST_C8_HITS = [
            false, false, false, false, false,
            false, false, false, false, false,
            false, true ];
    public static boolean[] GLOBAL_HITS = [
            true, true, true, true, true,
            true, true, false, true, false,
            true ];

    public static final int TEST_A_ID = 0;
    public static final int TEST_B_ID = 1;
    public static final int TEST_C_ID = 2;

    private final CoverageRecorder coverageRecorder;

    RunTestsOutOfOrderMTC(final CoverageRecorder coverageRecorder) {
        this.coverageRecorder = coverageRecorder;
    }

    void thread1() {
        waitForTick(1);
        coverageRecorder.sliceStart("MyClass", 0, TEST_A_ID, 0);

        waitForTick(3);
        coverageRecorder.inc(0);
        coverageRecorder.inc(2);
        coverageRecorder.inc(4);
        coverageRecorder.inc(6);

        waitForTick(6);
        coverageRecorder.inc(8);

        waitForTick(7);
        coverageRecorder.sliceEnd("MyClass", "MyClass.testA", "MyClass testA runtime name", 0, TEST_A_ID, 0, 0, null);
    }

    void thread2() {
        waitForTick(2);
        coverageRecorder.sliceStart("MyClass", 0, TEST_B_ID, 0);

        waitForTick(3);
        coverageRecorder.inc(0);
        coverageRecorder.inc(1);
        coverageRecorder.inc(4);
        coverageRecorder.inc(5);

        waitForTick(4);
        coverageRecorder.sliceEnd("MyClass", "MyClass.testB", "MyClass testB runtime name", 0, TEST_B_ID, 0, 0, null);
    }

    void thread3() {
        waitForTick(5);
        coverageRecorder.sliceStart("MyClass", 0, TEST_C_ID, 0);

        waitForTick(6);
        coverageRecorder.inc(0);
        coverageRecorder.inc(1);
        coverageRecorder.inc(2);
        coverageRecorder.inc(3);
        coverageRecorder.inc(10);

        waitForTick(8);
        coverageRecorder.inc(11);

        waitForTick(9);
        coverageRecorder.sliceEnd("MyClass", "MyClass.testC", "MyClass testC runtime name", 0, TEST_C_ID, 0, 0, null);
    }

    @Override
    void assertPerTestHitCounts(final int testCaseNumber, final CloverBitSet perTestCoverage,
                                CoverageAssertionAccuracy accuracy) {
        boolean[] minimumHitMask, maximumHitMask;

        /* calculate expected minimum and maximum code coverage */
        switch (testCaseNumber) {
            case TEST_A_ID:
                minimumHitMask = or(TEST_A3_HITS, TEST_A6_HITS);
                maximumHitMask = or(TEST_A3_HITS, TEST_A6_HITS, TEST_B_HITS, TEST_C6_HITS);
                break;
            case TEST_B_ID:
                minimumHitMask = TEST_B_HITS;
                maximumHitMask = or(TEST_A3_HITS, TEST_B_HITS);
                break;
            case TEST_C_ID:
                minimumHitMask = or(TEST_C6_HITS, TEST_C8_HITS);
                maximumHitMask = or(TEST_C6_HITS, TEST_C8_HITS, TEST_A6_HITS);
                break;
            default:
                fail("Invalid test case number: $testCaseNumber" as String);
                return;
        }

        switch (accuracy) {
            case CoverageAssertionAccuracy.EXACT:
                RecorderTestUtil.assertPerTestHitCounts("Test #$testCaseNumber at most: " as String, maximumHitMask, perTestCoverage);
                break;
            case CoverageAssertionAccuracy.MIN_MAX:
                RecorderTestUtil.assertPerTestHitCountsAtLeast("Test #$testCaseNumber at least: " as String, minimumHitMask, perTestCoverage);
                RecorderTestUtil.assertPerTestHitCountsAtMost("Test #$testCaseNumber at most: " as String, maximumHitMask, perTestCoverage);
                break;
        }
    }

    @Override
    void assertGlobalHitCounts(GlobalCoverageRecordingTranscript globalCoverage) {
        RecorderTestUtil.assertGlobalHitCounts("GLOBAL:", GLOBAL_HITS, globalCoverage);
    }

    @Override
    void assertGlobalAndPerTestRecordings(RecordingTranscripts.Filter filter) throws IOException {
        for (RecordingTranscripts.FileRef file : filter.getCoverageRecordingFiles()) {
            final GlobalCoverageRecordingTranscript recording = (GlobalCoverageRecordingTranscript)file.read(new CoverageDataSpec());
            assertGlobalHitCounts(recording);
        }

        for (RecordingTranscripts.FileRef file : filter.getPerTestRecordingFiles()) {
            final PerTestRecordingTranscript recording = (PerTestRecordingTranscript)file.read(new CoverageDataSpec());
            final CloverBitSet bitSet = recording.getCoverage();
            assertPerTestHitCounts(file.getTestId(), bitSet, CoverageAssertionAccuracy.EXACT);
        }
    }

    @Override
    void assertGlobalAndPerTestRecordingsInRange(RecordingTranscripts.Filter filter) throws IOException {
        for (RecordingTranscripts.FileRef file : filter.getCoverageRecordingFiles()) {
            final GlobalCoverageRecordingTranscript recording = (GlobalCoverageRecordingTranscript)file.read(new CoverageDataSpec());
            assertGlobalHitCounts(recording);
        }

        for (RecordingTranscripts.FileRef file : filter.getPerTestRecordingFiles()) {
            final PerTestRecordingTranscript recording = (PerTestRecordingTranscript)file.read(new CoverageDataSpec());
            final CloverBitSet bitSet = recording.getCoverage();
            assertPerTestHitCounts(file.getTestId(), bitSet, CoverageAssertionAccuracy.MIN_MAX);
        }
    }
}
