package com.atlassian.clover.recorder

import com.atlassian.clover.CoverageDataSpec
import org.openclover.runtime.Logger
import com.atlassian.clover.util.FileUtils
import org.openclover.runtime.util.IOStreamUtils
import org_openclover_runtime.Clover
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.*

class GlobalCoverageRecordingTest {
    private File recDir
    private CoverageDataSpec spec

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() throws Exception {
        recDir = File.createTempFile(getClass().getName() + "." + testName.methodName, "dir")
        recDir.delete()
        recDir.mkdir()
        spec = new CoverageDataSpec()
    }

    @After
    void tearDown() throws Exception {
        FileUtils.deltree(recDir)
    }

    @Test
    void testUnversionedRecFile() throws IOException {
        String recname = Clover.getRecordingName(hashCode(), "testrec", System.currentTimeMillis())
        File recFile = new File(recDir, recname)

        int[] elements = new int[40]

        long dbversion = 1L
        long ts = System.currentTimeMillis()
        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath(), dbversion, ts, elements)

        final int bytesWithoutMagixAndVersion = (int)recFile.length() - 12
        byte[] bytes = new byte[bytesWithoutMagixAndVersion]

        DataInputStream input = new DataInputStream(IOStreamUtils.createInflaterInputStream(recFile))
        input.readLong(); //Discard magic
        input.readInt();  //Discard version
        input.readFully(bytes)
        input.close()
        DataOutputStream out = new DataOutputStream(IOStreamUtils.createDeflateOutputStream(recFile))
        out.write(bytes)
        out.close()

        try {
            RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)
            fail()
        } catch (IOException e) {
        }
    }

    @Test
    void testNonAltWriteRead() throws IOException {
        String recname = Clover.getRecordingName(hashCode(), "testrec", System.currentTimeMillis())
        File recFile = new File(recDir, recname)

        int[] elements = new int[40]

        long dbversion = 1L
        long ts = System.currentTimeMillis()
        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath(), dbversion, ts, elements)

        GlobalCoverageRecordingTranscript read = RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)

        assertEquals(dbversion, read.getDbVersion())
        assertEquals(ts, read.getWriteTimeStamp())
        assertEquals(0, read.getCoverageSum())
    }

    @Test
    void testAltWriteRead() throws IOException {
        String recname = Clover.getRecordingName(hashCode(), "testrec", System.currentTimeMillis())
        File recFile = new File(recDir, recname)

        int[] elements = new int[40]

        int totalsize = elements.length

        long dbversion = 1L
        long ts = System.currentTimeMillis()
        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath(), dbversion, ts, elements)

        Arrays.fill(elements, 1)

        ts += 1

        FileBasedGlobalCoverageRecording.flushToDisk(
                recFile.getAbsolutePath() + GlobalCoverageRecording.ALT_SUFFIX, dbversion, ts, elements)

        GlobalCoverageRecordingTranscript read = RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)

        assertEquals(dbversion, read.getDbVersion())
        assertEquals(ts, read.getWriteTimeStamp())
        assertEquals(totalsize, read.getCoverageSum())

        Arrays.fill(elements, 2)

        ts += 1

        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath(), dbversion, ts, elements)

        read = RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)

        assertEquals(dbversion, read.getDbVersion())
        assertEquals(ts, read.getWriteTimeStamp())
        assertEquals(2 * totalsize, read.getCoverageSum())
    }

    @Test
    void testCorruptAltRead() throws IOException {
        Logger.setDebug(true)
        String recname = Clover.getRecordingName(hashCode(), "testrec", System.currentTimeMillis())
        File recFile = new File(recDir, recname)

        // corrupt primary header, noncorrupt alt: check that the alt is read
        recFile.createNewFile()

        int[] elements = new int[40]

        long dbversion = 1L
        long ts = System.currentTimeMillis()
        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath() + GlobalCoverageRecording.ALT_SUFFIX, dbversion, ts,
                elements)

        GlobalCoverageRecordingTranscript read = RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)

        assertEquals(dbversion, read.getDbVersion())
        assertEquals(ts, read.getWriteTimeStamp())
        assertEquals(0, read.getCoverageSum())

        // ok primary, alt has newer ts in header but corrupt data: check primary is read
        ts = System.currentTimeMillis()
        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath(), dbversion, ts, elements)

        writeCorruptRecording(recFile.getAbsolutePath() + GlobalCoverageRecording.ALT_SUFFIX, 10, dbversion, ts + 1)
        read = RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)

        assertEquals(dbversion, read.getDbVersion())
        assertEquals(ts, read.getWriteTimeStamp())
        assertEquals(0, read.getCoverageSum())

        // primary has newer ts in header but corrupt data: check alt is read
        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath() + GlobalCoverageRecording.ALT_SUFFIX, dbversion, ts,
                elements)

        writeCorruptRecording(recFile.getAbsolutePath(), 10, dbversion, ts + 1)
        read = RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)

        assertEquals(dbversion, read.getDbVersion())
        assertEquals(ts, read.getWriteTimeStamp())
        assertEquals(0, read.getCoverageSum())
    }

    @Test
    void testRLECompression() throws IOException {
        Random rnd = new Random()

        // large random
        int[] elements = new int[500000]
        long sum = 0
        for (int i = 0; i < elements.length; i++) {
            elements[i] = rnd.nextInt()
            sum += elements[i]
        }
        writeReadCheckData(elements, sum)

        // single value
        elements = [1] as int[]
        writeReadCheckData(elements, 1)

        // single run
        elements = [1, 1, 1] as int[]
        writeReadCheckData(elements, 3)

        // starts with run
        elements = [1, 1, 1, 2, 3, 2, 3, 2] as int[]
        writeReadCheckData(elements, calcSum(elements))

        // ends with run
        elements = [2, 3, 2, 3, 2, 1, 1, 1] as int[]
        writeReadCheckData(elements, calcSum(elements))

        // no runs
        elements = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9] as int[]
        writeReadCheckData(elements, calcSum(elements))

        // all run
        elements = [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1] as int[]
        writeReadCheckData(elements, calcSum(elements))

        // mostly run
        elements = [1, 1, 1, 3, 1, 1, 1, 2, 1, 1, 1] as int[]
        writeReadCheckData(elements, calcSum(elements))
    }

    private long calcSum(int[] data) {
        long result = 0
        for (int val : data) {
            result += val
        }
        return result
    }

    private void writeReadCheckData(int[] orig, long sum) throws IOException {
        String recname = Clover.getRecordingName(hashCode(), "testrec", System.currentTimeMillis())
        File recFile = new File(recDir, recname)
        long dbversion = 1L
        long ts = System.currentTimeMillis()
        FileBasedGlobalCoverageRecording.flushToDisk(recFile.getAbsolutePath(), dbversion, ts, orig)

        GlobalCoverageRecordingTranscript read = RecordingTranscripts.readCoverageFromDisk(recDir, recname, spec)

        assertEquals(dbversion, read.getDbVersion())
        assertEquals(ts, read.getWriteTimeStamp())
        assertEquals(sum, read.getCoverageSum())
        for (int i = 0; i < orig.length; i++) {
            assertEquals("divergence at position $i", orig[i], read.get(i))
        }
    }

    private void writeCorruptRecording(String dbname, int size, long version, long ts) throws IOException {
        // writes a header only.
        DataOutputStream out = new DataOutputStream(IOStreamUtils.createDeflateOutputStream(new File(dbname)))
        out.writeLong(BaseCoverageRecording.Header.REC_MAGIC)
        out.writeInt(BaseCoverageRecording.Header.REC_FORMAT_VERSION)
        out.writeLong(version)
        out.writeLong(ts)
        out.writeInt(size)
        out.close()
    }
}
