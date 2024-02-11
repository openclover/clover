package org.openclover.core.recorder

import com.atlassian.clover.recorder.GlobalCoverageRecordingTranscript
import com.atlassian.clover.recorder.RecordingTranscripts
import org.openclover.runtime.api.CloverException
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.util.FileUtils
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder
import org.openclover.runtime.recorder.NullRecorder
import org.openclover.runtime.registry.format.RegAccessMode

import org_openclover_runtime.CoverageRecorder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.*

class FixedSizeCoverageRecorderTest {
    File recDir
    File testDb
    long dbVersion
    CoverageDataSpec spec

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() throws Exception {
        recDir = File.createTempFile(getClass().getName() + "." + testName.methodName, "dir")
        recDir.delete()
        recDir.mkdir()
        dbVersion = System.currentTimeMillis()
        testDb = new File(recDir, "testdb")
        spec = new CoverageDataSpec()
    }

    @After
    void tearDown() throws Exception {
        FileUtils.deltree(recDir)
    }

    @Test
    void testDirectedFlushing() throws Exception {
        FixedSizeCoverageRecorder recorder = new FixedSizeCoverageRecorder(testDb.getAbsolutePath(), dbVersion, 4, 0L)
        recorder.startRun()
        File recordingFile = new File(recorder.getRecordingName())
        Thread shutdownFlusher = recorder.getShutdownFlusher()
        Runtime.getRuntime().removeShutdownHook(shutdownFlusher)

        recorder.forceFlush()

        GlobalCoverageRecordingTranscript recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(0, recording.getCoverageSum())

        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)

        recorder.forceFlush()

        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(4, recording.getCoverageSum())

        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)
        // emulate jvm shutdown by running the shutdown flusher.
        shutdownFlusher.start()
        shutdownFlusher.join()
        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(8, recording.getCoverageSum())
    }

    @Test
    void testIntervalFlushing() throws Exception {
        int interval = 2000
        FixedSizeCoverageRecorder recorder = new FixedSizeCoverageRecorder(testDb.getAbsolutePath(), dbVersion, 4,
            CoverageRecorder.getConfigBits(CoverageRecorder.FLUSHPOLICY_INTERVAL, interval, false, false, false))
        recorder.startRun()
        Thread shutdownFlusher = recorder.getShutdownFlusher()
        Runtime.getRuntime().removeShutdownHook(shutdownFlusher)

        File recordingFile = new File(recorder.getRecordingName())

        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)
        recorder.maybeFlush(); // the first flush

        GlobalCoverageRecordingTranscript recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(4, recording.getCoverageSum())
        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)
        recorder.maybeFlush(); // shouldn't flush

        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(4, recording.getCoverageSum())

        Thread.sleep(interval * 2)

        recorder.maybeFlush(); // should flush
        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(8, recording.getCoverageSum())

        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)

        // emulate jvm shutdown by running the shutdown flusher.
        shutdownFlusher.start()
        shutdownFlusher.join()
        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(12, recording.getCoverageSum())
    }

    @Test
    void testThreadedFlushing() throws Exception {
        long version = System.currentTimeMillis()
        File db = new File(recDir, "testdb")
        int interval = 200
        FixedSizeCoverageRecorder recorder = new FixedSizeCoverageRecorder(db.getAbsolutePath(), version, 4,
            CoverageRecorder.getConfigBits(CoverageRecorder.FLUSHPOLICY_THREADED, interval, false, false, false))
        recorder.startRun()
        Thread shutdownFlusher = recorder.getShutdownFlusher()
        Runtime.getRuntime().removeShutdownHook(shutdownFlusher)

        File recordingFile = new File(recorder.getRecordingName())

        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)
        recorder.flushNeeded()
        Thread.sleep(interval * 10); // atleast one flush event will occur
        GlobalCoverageRecordingTranscript recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)

        assertEquals(4, recording.getCoverageSum())

        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)
        recorder.flushNeeded()
        Thread.sleep(interval * 10); // atleast one flush event will occur
        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)
        long rects0 = recording.getWriteTimeStamp()
        // check a flush occured
        assertEquals(8, recording.getCoverageSum())

        Thread.sleep(interval * 10); // atleast one flush event will occur

        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)
        // no coverage accumulated, check a flush didn't occur
        assertEquals(rects0, recording.getWriteTimeStamp())
        assertEquals(8, recording.getCoverageSum())

        recorder.inc(0)
        recorder.inc(1)
        recorder.inc(2)
        recorder.inc(3)

        // emulate jvm shutdown by running the shutdown flusher.
        shutdownFlusher.start()
        shutdownFlusher.join()
        recording = RecordingTranscripts.readCoverageFromDisk(recordingFile, spec)
        assertEquals(12, recording.getCoverageSum())
    }

    @Test
    void testNullRecorderWhenCapacityInsufficient() {
        CoverageRecorder recorder = new FixedSizeCoverageRecorder("foo", 0, 1000, 0L)
        assertSame(NullRecorder.INSTANCE, recorder.withCapacityFor(1001))
        assertSame(NullRecorder.INSTANCE, recorder.withCapacityFor(2001))
    }

    @Test
    void testThisRecorderReturnedWhenCapacitySufficient() {
        CoverageRecorder recorder = new FixedSizeCoverageRecorder("foo", 0, 1000, 0L)
        assertSame(recorder, recorder.withCapacityFor(1000))
        assertSame(NullRecorder.INSTANCE, recorder.withCapacityFor(1001))
        assertSame(recorder, recorder.withCapacityFor(1000))
    }

    @Test
    void testNullRecorderReturnedWhenMergedDatabaseUsed() throws IOException, CloverException {
        Clover2Registry reg = new Clover2Registry(testDb, RegAccessMode.READONLY, testName.methodName)
        reg.saveAndOverwriteFile()
        assertSame(NullRecorder.INSTANCE, FixedSizeCoverageRecorder.createFor(testDb, reg.getVersion(), 1000, 0L))
    }
}
