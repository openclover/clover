package org.openclover.core.recorder

import org.openclover.core.CoverageDataSpec
import org.openclover.runtime.util.CloverBitSet
import org_openclover_runtime.CoverageRecorder

/**
 * Test case which runs three threads. Each thread runs a single test case (i.e. test start/end detected by Clover).
 * These test cases start and end in different order. Code coverage from test cases is as follows:
 * <pre>
 *     TestA TestB TestC      Global
 *  0
 *  1          x                x
 *  2    x     x     x          x
 *  3    x     x                x
 *  4          x     x          x
 *  5
 *  6                x          x
 *  7                x          x
 *  8                x          x
 *  9
 * </pre>
 *
 * Order in which test cases start (s) and end (e) and coverage is written (*):
 * <pre>
 *    time    0   1   2   3   4   5   6   7   8
 *    TestA   s ----------*-- e
 *    TestB       s ------*-------*-- e
 *    TestC           s --*---------------*-- e
 * </pre>
 *
 * Expected result: each test case gets coverage generated from all running threads at time when the test was active
 * <pre>
 *    TestA gets hits from time=3   = minimum A, maximum A+B'+C'
 *    TestB gets hits from time=3+5 = minimum B, maximum A+B+C'
 *    TestC gets hits from time=3+5 = minimum C, maximum A+B+C
 * </pre>
 */
public class RunTestsInOrderMTC extends RunTestsMTC {

    /** Expected values */
    public static boolean[] TEST_A_HITS = [
            false, false, true, true, false,
            false, false, false, false, false ];
    public static boolean[] TEST_B3_HITS = [
            false, true, true, true, false,
            false, false, false, false, false ];
    public static boolean[] TEST_B5_HITS = [
            false, false, false, false, true,
            false, false, false, false, false ];
    public static boolean[] TEST_C3_HITS = [
            false, false, true, false, false,
            false, false, false, false, false ];
    public static boolean[] TEST_C7_HITS = [
            false, false, false, false, true,
            false, true, true, true, false ];
    public static boolean[] GLOBAL_HITS = [
            false, true, true, true, true,
            false, true, true, true, false ];

    public static final int TEST_A_ID = 0;
    public static final int TEST_B_ID = 1;
    public static final int TEST_C_ID = 2;

    private final CoverageRecorder coverageRecorder;

    public RunTestsInOrderMTC(final CoverageRecorder coverageRecorder) {
        this.coverageRecorder = coverageRecorder;
    }

    public void thread1() {
        waitForTick(0);
        coverageRecorder.sliceStart("MyClass", 0L, TEST_A_ID, 0);

        waitForTick(3);
        coverageRecorder.inc(2);
        coverageRecorder.inc(3);

        waitForTick(4);
        coverageRecorder.sliceEnd("MyClass", "MyClass.testA", "MyClass testA runtime name", 0L, TEST_A_ID, 0, 0, null);
    }

    public void thread2() {
        waitForTick(1);
        coverageRecorder.sliceStart("MyClass", 0L, TEST_B_ID, 0);

        waitForTick(3);
        coverageRecorder.inc(1);
        coverageRecorder.inc(2);
        coverageRecorder.inc(3);

        waitForTick(5);
        coverageRecorder.inc(4);

        waitForTick(6);
        coverageRecorder.sliceEnd("MyClass", "MyClass.testB", "MyClass testB runtime name", 0L, TEST_B_ID, 0, 0, null);
    }

    public void thread3() {
        waitForTick(2);
        coverageRecorder.sliceStart("MyClass", 0L, TEST_C_ID, 0);

        waitForTick(3);
        coverageRecorder.inc(2);

        waitForTick(7);
        coverageRecorder.inc(4);
        coverageRecorder.inc(6);
        coverageRecorder.inc(7);
        coverageRecorder.inc(8);

        waitForTick(8);
        coverageRecorder.sliceEnd("MyClass", "MyClass.testC", "MyClass testC runtime name", 0L, TEST_C_ID, 0, 0, null);
    }

    @Override
    public void assertPerTestHitCounts(final int testCaseNumber, final CloverBitSet perTestCoverage, CoverageAssertionAccuracy accuracy) {
        boolean[] minimumHitMask, maximumHitMask;
        switch (testCaseNumber) {
            case TEST_A_ID:
                minimumHitMask = TEST_A_HITS;
                maximumHitMask = or(TEST_A_HITS, TEST_B3_HITS, TEST_C3_HITS);
                break;
            case TEST_B_ID:
                minimumHitMask = or(TEST_B3_HITS, TEST_B5_HITS);
                maximumHitMask = or(TEST_B3_HITS, TEST_B5_HITS, TEST_A_HITS, TEST_C3_HITS);
                break;
            case TEST_C_ID:
                minimumHitMask = or(TEST_C3_HITS, TEST_C7_HITS);
                maximumHitMask = or(TEST_A_HITS, TEST_B3_HITS, TEST_B5_HITS, TEST_C3_HITS, TEST_C7_HITS);
                break;
            default:
                fail("Invalid test case number: $testCaseNumber".toString());
                return;
        }

//        RecorderTestUtil.assertPerTestHitCountsAtLeast("Test #" + testCaseNumber + " at least:", minimumHitMask, perTestCoverage);
        RecorderTestUtil.assertPerTestHitCounts("Test #$testCaseNumber at most:".toString(), maximumHitMask, perTestCoverage);
    }

    @Override
    public void assertGlobalHitCounts(GlobalCoverageRecordingTranscript globalCoverage) {
        RecorderTestUtil.assertGlobalHitCounts("GLOBAL:", GLOBAL_HITS, globalCoverage);
    }

    @Override
    public void assertGlobalAndPerTestRecordings(RecordingTranscripts.Filter filter) throws IOException {
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
    public void assertGlobalAndPerTestRecordingsInRange(RecordingTranscripts.Filter filter) throws IOException {
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
