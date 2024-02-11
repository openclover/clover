package com.atlassian.clover.recorder

import org.openclover.runtime.RuntimeType
import org.openclover.buildutil.testutils.IOHelper
import org.openclover.runtime.recorder.FileBasedPerTestRecording
import org.openclover.runtime.util.CloverBitSet
import com.atlassian.clover.util.collections.Pair
import org_openclover_runtime.Clover
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.*

class RecordingTranscriptsTest {
    private File tmpDir
    private final String basename = "clover.db"
    private long currentMonotonicTime

    @Rule
    public TestName testName = new TestName()
    
    @Before
    void setUp() throws Exception {
        tmpDir = IOHelper.createTmpDir(testName.methodName)
        final long ct = System.currentTimeMillis()
        if (ct > currentMonotonicTime) {
            currentMonotonicTime = ct
        }
    }

    @After
    void tearDown() throws Exception {
        if (!IOHelper.delete(tmpDir)) {
            throw new RuntimeException("Unable to delete temporary test directory ${tmpDir.absolutePath}".toString())
        }
    }

    @Test
    void testCollectUnseenFilesWithPerTest() throws IOException {
        RecordingTranscripts.Filter emptyFilter = new RecordingTranscripts.Filter(tmpDir, basename, 0, Long.MAX_VALUE, false, true)
        emptyFilter.collectAllFiles()

        assertTrue(emptyFilter.getCoverageRecordingFiles().isEmpty())
        assertTrue(emptyFilter.getPerTestRecordingFiles().isEmpty())

        createRecordingFile()
        Pair<Set<RecordingTranscripts.FileRef>, Set<RecordingTranscripts.FileRef>> newRecordings =
            emptyFilter.collectUnseenFilesAnd(emptyFilter)

        assertEquals(1, emptyFilter.getCoverageRecordingFiles().size())
        assertEquals(1, newRecordings.first.size())

        assertEquals(0, emptyFilter.getPerTestRecordingFiles().size())
        assertEquals(0, newRecordings.second.size())

        createRecordingFile()
        createPerTestFiles(2)

        newRecordings = emptyFilter.collectUnseenFilesAnd(emptyFilter)

        assertEquals(2, emptyFilter.getCoverageRecordingFiles().size())
        assertEquals(1, newRecordings.first.size())

        assertEquals(2, emptyFilter.getPerTestRecordingFiles().size())
        assertEquals(2, newRecordings.second.size())

        createPerTestFiles(5)

        newRecordings = emptyFilter.collectUnseenFilesAnd(emptyFilter)

        assertEquals(2, emptyFilter.getCoverageRecordingFiles().size())
        assertEquals(0, newRecordings.first.size())

        assertEquals(7, emptyFilter.getPerTestRecordingFiles().size())
        assertEquals(5, newRecordings.second.size())
    }

    @Test
    void testCollectUnseenFilesWithoutPerTest() throws IOException {
        RecordingTranscripts.Filter emptyFilter = new RecordingTranscripts.Filter(tmpDir, basename, 0, Long.MAX_VALUE, false, false)
        emptyFilter.collectAllFiles()

        assertTrue(emptyFilter.getCoverageRecordingFiles().isEmpty())
        assertTrue(emptyFilter.getPerTestRecordingFiles().isEmpty())

        createRecordingFile()
        Pair<Set<RecordingTranscripts.FileRef>, Set<RecordingTranscripts.FileRef>> newRecordings =
            emptyFilter.collectUnseenFilesAnd(emptyFilter)

        assertEquals(1, emptyFilter.getCoverageRecordingFiles().size())
        assertEquals(1, newRecordings.first.size())

        assertEquals(0, emptyFilter.getPerTestRecordingFiles().size())
        assertEquals(0, newRecordings.second.size())

        createRecordingFile()
        createPerTestFiles(2)

        newRecordings = emptyFilter.collectUnseenFilesAnd(emptyFilter)

        assertEquals(2, emptyFilter.getCoverageRecordingFiles().size())
        assertEquals(1, newRecordings.first.size())

        assertEquals(0, emptyFilter.getPerTestRecordingFiles().size())
        assertEquals(0, newRecordings.second.size())

        createPerTestFiles(5)

        newRecordings = emptyFilter.collectUnseenFilesAnd(emptyFilter)

        assertEquals(2, emptyFilter.getCoverageRecordingFiles().size())
        assertEquals(0, newRecordings.first.size())

        assertEquals(0, emptyFilter.getPerTestRecordingFiles().size())
        assertEquals(0, newRecordings.second.size())
    }

    @Test
    void testCollectUnseenFilesMixed() throws IOException {
        createRecordingFile()
        createPerTestFiles(2)

        RecordingTranscripts.Filter filter1 = new RecordingTranscripts.Filter(tmpDir, basename, 0, Long.MAX_VALUE, false, false)
        filter1.collectAllFiles()

        assertEquals(1, filter1.getCoverageRecordingFiles().size())
        assertEquals(0, filter1.getPerTestRecordingFiles().size())

        createRecordingFile()
        createPerTestFiles(2)

        RecordingTranscripts.Filter filter2 = new RecordingTranscripts.Filter(tmpDir, basename, 0, Long.MAX_VALUE, false, true)
        Pair<Set<RecordingTranscripts.FileRef>, Set<RecordingTranscripts.FileRef>> newRecordings = filter2.collectUnseenFilesAnd(filter1)

        assertEquals(2, filter2.getCoverageRecordingFiles().size())
        assertEquals(4, filter2.getPerTestRecordingFiles().size())
        assertEquals(1, newRecordings.first.size())
        assertEquals(4, newRecordings.second.size())

        createRecordingFile()
        createPerTestFiles(2)

        RecordingTranscripts.Filter filter3 = new RecordingTranscripts.Filter(tmpDir, basename, 0, Long.MAX_VALUE, false, false)
        newRecordings = filter3.collectUnseenFilesAnd(filter2)

        assertEquals(3, filter3.getCoverageRecordingFiles().size())
        assertEquals(4, filter3.getPerTestRecordingFiles().size())
        assertEquals(1, newRecordings.first.size())
        assertEquals(0, newRecordings.second.size())

        createRecordingFile()
        createPerTestFiles(2)

        RecordingTranscripts.Filter filter4 = new RecordingTranscripts.Filter(tmpDir, basename, 0, Long.MAX_VALUE, false, true)
        newRecordings = filter4.collectUnseenFilesAnd(filter3)

        assertEquals(4, filter4.getCoverageRecordingFiles().size())
        assertEquals(8, filter4.getPerTestRecordingFiles().size())
        assertEquals(1, newRecordings.first.size())
        assertEquals(4, newRecordings.second.size())
    }

    @Test
    void testIsOutOfDate() throws IOException {
        RecordingTranscripts.Filter filter = new RecordingTranscripts.Filter(tmpDir, basename, 0, Long.MAX_VALUE, false, true)
        assertFalse(filter.isOutOfDate())

        createRecordingFile()
        assertTrue(filter.isOutOfDate())
        assertTrue(filter.isOutOfDate())

        filter.collectUnseenFilesAnd(filter)
        assertFalse(filter.isOutOfDate())

        createRecordingFile()
        assertTrue(filter.isOutOfDate())

    }

    private void createPerTestFiles(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            CloverBitSet coverage = new CloverBitSet()
            new FileBasedPerTestRecording(new File(tmpDir, basename).getAbsolutePath(), 1, 0,
                    coverage, "testMethod", "testMethodAtRuntime",
                    getCurrentTime(), getCurrentTime(), 0.001,
                    new RuntimeType("TestClass"), i, 0, 0, null).transcribe()
        }
    }

    private void createRecordingFile() throws IOException {
        String recname = Clover.getRecordingName(hashCode(), new File(tmpDir, basename).getAbsolutePath(), getCurrentTime())
        FileBasedGlobalCoverageRecording.flushToDisk(recname, 1, getCurrentTime(), new int[1])
    }

    private long getCurrentTime() {
        return currentMonotonicTime++; 
    }

}
