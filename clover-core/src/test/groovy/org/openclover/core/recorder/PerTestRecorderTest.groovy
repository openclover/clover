package org.openclover.core.recorder

import org.openclover.runtime.ErrorInfo
import org_openclover_runtime.Clover
import org_openclover_runtime.CoverageRecorder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.runtime.recorder.ActivePerTestRecorderAny
import org.openclover.runtime.recorder.ActivePerTestRecorderMany
import org.openclover.runtime.recorder.ActivePerTestRecorderNone
import org.openclover.runtime.recorder.ActivePerTestRecorderOne
import org.openclover.runtime.recorder.BaseCoverageRecorder
import org.openclover.runtime.recorder.FileBasedPerTestRecording
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder
import org.openclover.runtime.recorder.LivePerTestRecording
import org.openclover.runtime.recorder.PerTestRecorder
import org.openclover.runtime.recorder.RecordingResult

import java.lang.reflect.Field

import static org.junit.Assert.*

/**
 * Test for {@link PerTestRecorder}
 */
class PerTestRecorderTest {

    @Rule
    public TestName testName = new TestName()

    @Test
    void testNullRecording() {
        PerTestRecorder.Null recorder = new PerTestRecorder.Null()
        recorder.set(-1)
        recorder.set(Integer.MIN_VALUE)
        recorder.set(0)
        recorder.set(1)
        recorder.set(Integer.MAX_VALUE)
        assertSame(recorder.testFinished(null, null, null, 0, 0, 0, 0, null), LivePerTestRecording.NULL)
    }

    @Test
    void testDiffRecording() throws NoSuchFieldException, IllegalAccessException {
        final long start = System.currentTimeMillis()

        final BaseCoverageRecorder recorder = newCoverageRecorder(6)
        final PerTestRecorder.Diffing testRecorder = new PerTestRecorder.Diffing(recorder)
        final Field testRecorderField = BaseCoverageRecorder.class.getDeclaredField("testCoverage")
        testRecorderField.setAccessible(true)
        testRecorderField.set(recorder, testRecorder)
        
        recorder.inc(4)
        testRecorder.testStarted(getClass().getName(), start, 0, 0)
        recorder.inc(0)
        recorder.inc(0)
        recorder.inc(0)
        recorder.inc(0)
        recorder.inc(2)
        recorder.inc(4)
        final FileBasedPerTestRecording recording = (FileBasedPerTestRecording)testRecorder.testFinished(
                getClass().getName(), "test", null, System.currentTimeMillis(), 0, 0, 1, null)

        assertTrue(recording.get(0))
        assertFalse(recording.get(1))
        assertTrue(recording.get(2))
        assertFalse(recording.get(3))
        assertTrue(recording.get(4))
        assertFalse(recording.get(5))
    }

    @Test
    void testBalancedTestCompletion() {
        final int testRunId = Integer.MAX_VALUE
        long start = System.currentTimeMillis()
        int sliceId = 0

        //Tests starting
        ActivePerTestRecorderAny recorders = new ActivePerTestRecorderNone(newCoverageRecorder())

        recorders = recorders.testStarted(getClass().getName(), start++, sliceId++, testRunId)
        assertTrue("Inc(None) => One", recorders instanceof ActivePerTestRecorderOne)

        cover((sliceId - 1) * 5, 5, recorders)

        recorders = recorders.testStarted(getClass().getName(), start++, sliceId++, testRunId)
        assertTrue("Inc(One) => Many{2}", recorders instanceof ActivePerTestRecorderMany)

        cover((sliceId - 1) * 5, 5, recorders)

        recorders = recorders.testStarted(getClass().getName(), start++, sliceId++, testRunId)
        assertTrue("Inc(Many{2}) => Many{3}", recorders instanceof ActivePerTestRecorderMany)

        cover((sliceId - 1) * 5, 5, recorders)

        //Tests ending
        RecordingResult result = recorders.testFinished(getClass().getName(), "test" + (--sliceId), null,
                start++, sliceId, testRunId, 1, null)
        recorders = result.recorders
        assertTrue("Dec(Many{3}) => Many{2}", recorders instanceof ActivePerTestRecorderMany)
        assertCoverage("test" + sliceId, sliceId * 5, 5, (FileBasedPerTestRecording)result.recording)

        result = recorders.testFinished(getClass().getName(), "test" + (--sliceId), null,
                start++, sliceId, testRunId, 1, null)
        recorders = result.recorders
        assertTrue("Dec(Many{2}) => One", recorders instanceof ActivePerTestRecorderOne)
        assertCoverage("test" + sliceId, sliceId * 5, 10, (FileBasedPerTestRecording)result.recording)

        result = recorders.testFinished(getClass().getName(), "test" + (--sliceId), null,
                start++, sliceId, testRunId, 1, null)
        recorders = result.recorders
        assertTrue("Dec(One) => None", recorders instanceof ActivePerTestRecorderNone)
    }

    @Test
    void testOutOfOrderTestCompletion() {
        final int testRunId = Integer.MAX_VALUE
        final long start = System.currentTimeMillis()
        long when = start
        final int startSliceId = 0
        int sliceId = startSliceId

        //Tests start
        CoverageRecorder coverageRecorder = newCoverageRecorder()
        ActivePerTestRecorderAny recorders = new ActivePerTestRecorderNone(coverageRecorder)

        recorders = recorders.testStarted(getClass().getName(), when++, sliceId++, testRunId)
        assertTrue("Inc(None) => One", recorders instanceof ActivePerTestRecorderOne)

        cover((sliceId - 1) * 5, 5, recorders)

        recorders = recorders.testStarted(getClass().getName(), when++, sliceId++, testRunId)
        assertTrue("Inc(One) => Many{2}", recorders instanceof ActivePerTestRecorderMany)

        cover((sliceId - 1) * 5, 5, recorders)

        recorders = recorders.testStarted(getClass().getName(), when++, sliceId++, testRunId)
        assertTrue("Inc(Many{2}) => Many{3}", recorders instanceof ActivePerTestRecorderMany)

        cover((sliceId - 1) * 5, 5, recorders)

        //Tests end out of order

        final ErrorInfo errorInfo = new ErrorInfo("bad!", "")

        RecordingResult result = recorders.testFinished(getClass().getName(), "test" + 0, null,
                when++, 0, testRunId, 0, errorInfo)
        recorders = result.recorders
        assertNonNullRecording(result)
        assertRecordingGeneratedIs(coverageRecorder, result.recording, getClass(), "test" + 0, null,
                start + 0, when - 1, startSliceId + 0, testRunId, 0, errorInfo)
        assertTrue("Dec(Many{3}) => Many{2}", recorders instanceof ActivePerTestRecorderMany)
        assertCoverage("test" + 0, 0, 5, (FileBasedPerTestRecording)result.recording)

        result = recorders.testFinished(getClass().getName(), "test" + 1, null,
                when++, 1, testRunId, 1, null)
        recorders = result.recorders
        assertNonNullRecording(result)
        assertRecordingGeneratedIs(coverageRecorder, result.recording, getClass(), "test" + 1, null,
                start + 1, when - 1, startSliceId + 1, testRunId, 1, null)
        assertTrue("Dec(Many{2}) => One", recorders instanceof ActivePerTestRecorderOne)
        assertCoverage("test" + 1, 5, 5, (FileBasedPerTestRecording)result.recording)

        result = recorders.testFinished(getClass().getName(), "test" + 2, null,
                when++, 2, testRunId, 0, errorInfo)
        recorders = result.recorders
        assertNonNullRecording(result)
        assertRecordingGeneratedIs(coverageRecorder, result.recording, getClass(), "test" + 2, null,
                start + 2, when - 1, startSliceId + 2, testRunId, 0, errorInfo)
        assertTrue("Dec(One) => None", recorders instanceof ActivePerTestRecorderNone)
        assertCoverage("test" + 2, 10, 5, (FileBasedPerTestRecording)result.recording)
    }

    @Test
    void testUnexpectedTestFinishingWhenNoneAlreadyActive() {
        final ActivePerTestRecorderAny recorder = new ActivePerTestRecorderNone(newCoverageRecorder())
        RecordingResult result = recorder.testFinished(getClass().getName(), "test", null,
                System.currentTimeMillis(), 0, 0, 1, null)
        assertResultHasNullRecording(result)
        assertTrue(result.recorders instanceof ActivePerTestRecorderNone)
    }

    @Test
    void testUnexpectedTestFinishingWhenOneAlreadyActive() {
        ActivePerTestRecorderAny recorder = new ActivePerTestRecorderNone(newCoverageRecorder())
        recorder = recorder.testStarted(getClass().getName(), System.currentTimeMillis(), 0, 0)
        RecordingResult result = recorder.testFinished(getClass().getName(), "test", null,
                System.currentTimeMillis(), -1, -1, 1, null)
        assertResultHasNullRecording(result)
        assertTrue(result.recorders instanceof ActivePerTestRecorderOne)
        assertSame(result.recorders, recorder)
    }

    @Test
    void testUnexpectedTestFinishingWhenManyAlreadyActive() {
        ActivePerTestRecorderAny recorder = new ActivePerTestRecorderNone(newCoverageRecorder())
        recorder = recorder.testStarted(getClass().getName(), System.currentTimeMillis(), 0, 0)
        recorder = recorder.testStarted(getClass().getName(), System.currentTimeMillis(), 1, 0)
        recorder = recorder.testStarted(getClass().getName(), System.currentTimeMillis(), 2, 0)
        RecordingResult result = recorder.testFinished(getClass().getName(), "test", null,
                System.currentTimeMillis(), -1, -1, 1, null)
        assertResultHasNullRecording(result)
        assertTrue(result.recorders instanceof ActivePerTestRecorderMany)
        assertSame(result.recorders, recorder)
    }

    @Test
    void testCanRecordCoverageIfNoTestActive() {
        new ActivePerTestRecorderNone(newCoverageRecorder()).set(0)
    }

    private void assertRecordingGeneratedIs(CoverageRecorder coverageRecorder, LivePerTestRecording result,
                                            Class clazz, String method, String runtimeTestName,
                                            long start, long end, int slice, int testRunId,
                                            int exitStatus, ErrorInfo errorInfo) {
        assertEquals(result.getTestTypeName(), clazz.getName())
        assertEquals(result.getTestMethodName(), method)
        assertEquals(result.getRuntimeTestName(), runtimeTestName)
        assertEquals(result.getEnd(), end)
        assertEquals(result.getExitStatus(), exitStatus)
        assertEquals(errorInfo == null ? null : errorInfo.getMessage(), result.getExitMessage())
        assertEquals(
            Clover.getSliceRecordingName(Clover.getTypeID(clazz.getName()), slice, testRunId, coverageRecorder.hashCode(), coverageRecorder.getDbName(), start),
            ((FileBasedPerTestRecording)result).getFile().getName())
    }

    private void assertNonNullRecording(RecordingResult result) {
        assertTrue("Recording should not be null",  result.recording != LivePerTestRecording.NULL)
    }

    private void assertResultHasNullRecording(RecordingResult result) {
        assertTrue("Recording should be null", result.recording == LivePerTestRecording.NULL)
    }

    private void assertCoverage(String recordingDescription, int start, int count, FileBasedPerTestRecording recording) {
        for (int i = start; i < start + count; i++) {
            assertTrue("Index " + i + " of recording " + recordingDescription + " should have been set", recording.get(i))
        }
    }

    private CoverageRecorder newCoverageRecorder() {
        return newCoverageRecorder(1)
    }

    private BaseCoverageRecorder newCoverageRecorder(int numElements) {
        return new FixedSizeCoverageRecorder(
            testName.methodName, 0, numElements,
            CoverageRecorder.getConfigBits(CoverageRecorder.FLUSHPOLICY_DIRECTED, 0, false, true, true))
    }

    private void cover(int start, int count, ActivePerTestRecorderAny recorder) {
        for (int i = start; i < start + count; i++) {
            recorder.set(i)
        }
    }
}
