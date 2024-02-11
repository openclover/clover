package com.atlassian.clover

import org.openclover.runtime.ErrorInfo
import org.openclover.runtime.RuntimeType
import org.openclover.runtime.recorder.FileBasedPerTestRecording
import org.openclover.runtime.recorder.PerTestRecording
import com.atlassian.clover.recorder.PerTestRecordingTranscript
import com.atlassian.clover.recorder.RecordingTranscripts
import junit.framework.TestCase
import com_atlassian_clover.Clover

import com.atlassian.clover.util.CloverUtils
import com.atlassian.clover.util.FileUtils
import org.openclover.runtime.util.CloverBitSet

class SliceRecordingTest extends TestCase {
    private File recDir
    private CoverageDataSpec spec

    SliceRecordingTest(String name) {
        super(name)
    }

    protected void setUp() throws Exception {
        recDir = File.createTempFile(getClass().getName() + "." + getName(), "dir")
        recDir.delete()
        recDir.mkdir()
        spec = new CoverageDataSpec()
    }

    protected void tearDown() throws Exception {
        FileUtils.deltree(recDir)
    }

    void testFormatRoundTrip() throws Exception {
        Logger.setDebug(true)

        final long dbVersion = 1L
        final long start = System.currentTimeMillis()
        final long end = start + 10
        final ErrorInfo ei = new ErrorInfo("testing", "this is a stack trace.")
        final String stackTrace = CloverUtils.transformStackTrace(ei.getStackTrace(), false)
        final String message = ei.getMessage()
        final String method = "a.b.c.d"
        final String testName = "test#123"
        final int result = 0
        final RuntimeType thisType = new RuntimeType(getClass().getName())

        final String dbName = "testrec" + getName()
        final String dbPath = new File(recDir, dbName).getAbsolutePath()
        final String recorderName = Clover.getSliceRecordingName(thisType.id, -1, -1, hashCode(), "testrec" + getName(), start)

        final CloverBitSet coverage = new CloverBitSet()
        coverage.add(0)
        coverage.add(1)
        coverage.add(2)
        coverage.add(3)
        coverage.add(4)
        new FileBasedPerTestRecording(dbPath, dbVersion, hashCode(),
                coverage, method, testName, start, end, 0.01, thisType, -1, -1, result, ei).transcribe()

        spec.setFilterTraces(false)
        PerTestRecordingTranscript read = RecordingTranscripts.readSliceFromDisk(recDir, recorderName, spec)

        assertEquals(dbVersion, read.getDbVersion())
        assertEquals(end, read.getWriteTimeStamp())
        assertEquals(PerTestRecording.FORMAT, read.getFormat())
        assertEquals(getClass().getName(), read.getTestTypeName())
        assertEquals(method, read.getTestMethodName())
        assertEquals(testName, read.getRuntimeTestName())
        assertTrue(read.hasResult())
        assertFalse(read.isResultPassed())
        assertEquals(stackTrace, read.getStackTrace())
        assertEquals(message, read.getExitMessage())
    }
}
