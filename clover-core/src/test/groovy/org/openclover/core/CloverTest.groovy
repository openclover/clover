package org.openclover.core

import junit.framework.TestCase
import org.junit.Test
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.util.FileUtils
import org.openclover.runtime.CloverNames
import org.openclover.runtime.CloverProperties
import org.openclover.runtime.api.registry.CloverRegistryException
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder
import org.openclover.runtime.recorder.NullRecorder
import org.openclover.runtime.registry.format.RegAccessMode
import org_openclover_runtime.Clover
import org_openclover_runtime.CoverageRecorder

/**
 */
class CloverTest extends TestCase {
    @Test
    void testGetErrorInfo() {
        assertNull(Clover.getErrorInfo(null))
        Throwable e = new Exception("Testing")
        assertEquals(e.getMessage(), Clover.getErrorInfo(e).getMessage())
        assertEquals(Clover.stackTraceFor(e), Clover.getErrorInfo(e).getStackTrace())
    }

    @Test
    void testNullRecorderIfDBNotFound() {
        assertSame(NullRecorder.INSTANCE,
                Clover.createRecorder("no_such_recorder", 0, 0, 1000, null, CloverProperties.newEmptyProperties()))
    }

    @Test
    void testNullRecorderIfReadOnlyDb() throws IOException, CloverRegistryException {
        final File regFile = File.createTempFile("clover", "db", TestUtils.createEmptyDirFor(getClass(), getName()))
        regFile.deleteOnExit()
        new Clover2Registry(regFile, RegAccessMode.READONLY, getName()).saveAndOverwriteFile()
        assertSame(NullRecorder.INSTANCE,
                Clover.createRecorder(regFile.getAbsolutePath(), 0, 0, 0, null, CloverProperties.newEmptyProperties()))
    }

    @Test
    void testNullRecorderIfDbTooSmall() throws IOException, CloverRegistryException {
        final File regFile = File.createTempFile("clover", "db", TestUtils.createEmptyDirFor(getClass(), getName()))
        regFile.deleteOnExit()
        new Clover2Registry(regFile, getName()).saveAndOverwriteFile()
        final CoverageRecorder recorder = Clover.createRecorder(regFile.getAbsolutePath(),
                0, 0, 999, null, CloverProperties.newEmptyProperties())
        assertSame(NullRecorder.INSTANCE, recorder)
    }

    @Test
    void testNullRecorderIfDbInvalid() throws IOException, CloverRegistryException {
        final File tmpDir = TestUtils.createEmptyDirFor(getClass(), getName())
        final File regFile = File.createTempFile("clover", "db", tmpDir)
        regFile.deleteOnExit()
        new Clover2Registry(regFile, getName()).saveAndOverwriteFile()
        final File emptyFile = File.createTempFile("clover", "db", tmpDir)
        emptyFile.createNewFile()
        emptyFile.renameTo(regFile)
        final CoverageRecorder recorder = Clover.createRecorder(regFile.getAbsolutePath(),
                0, 0, 999, null, CloverProperties.newEmptyProperties())
        assertSame(NullRecorder.INSTANCE, recorder)
    }

    @Test
    void testFixedSizeRecorderIfDBFound() throws IOException, CloverRegistryException {
        final File regFile = File.createTempFile("clover", "db", TestUtils.createEmptyDirFor(getClass(), getName()))
        regFile.deleteOnExit()
        new Clover2Registry(regFile, getName()).saveAndOverwriteFile()
        final CoverageRecorder recorder = Clover.createRecorder(regFile.getAbsolutePath(),
                0, 0, -1, null, CloverProperties.newEmptyProperties())
        assertTrue(recorder instanceof FixedSizeCoverageRecorder)
    }

    @Test
    void testInitStringResolution() {
        final String origValue = System.getProperty(CloverNames.PROP_INITSTRING)
        try {
            System.setProperty(CloverNames.PROP_INITSTRING, "foo.db")
            assertEquals("foo.db", Clover.resolveRegistryFile("bar.db", CloverProperties.newEmptyProperties()).getPath())
        } finally {
            if (origValue == null) {
                System.clearProperty(CloverNames.PROP_INITSTRING)
            } else {
                System.setProperty(CloverNames.PROP_INITSTRING, origValue)
            }
        }
    }

    @Test
    void testInitStringBasedirResolution() throws IOException {
        final String origValue = System.getProperty(CloverNames.PROP_INITSTRING_BASEDIR)
        try {
            final File tempDir1 = FileUtils.createTempDir("foo")
            final File tempDir2 = FileUtils.createTempDir("foo")
            System.setProperty(CloverNames.PROP_INITSTRING_BASEDIR, tempDir1.getAbsolutePath())

            assertEquals(
                new File(tempDir1, "bar.db").getAbsolutePath(),
                Clover.resolveRegistryFile(new File(tempDir2, "bar.db").getAbsolutePath(), CloverProperties.newEmptyProperties()).getAbsolutePath())

            //Test that all paths on original initstring are lopped off
            assertEquals(
                new File(tempDir1, "bar.db").getAbsolutePath(),
                Clover.resolveRegistryFile(new File(new File(tempDir2, "foo"), "bar.db").getAbsolutePath(), CloverProperties.newEmptyProperties()).getAbsolutePath())
        } finally {
            if (origValue == null) {
                System.clearProperty(CloverNames.PROP_INITSTRING_BASEDIR)
            } else {
                System.setProperty(CloverNames.PROP_INITSTRING_BASEDIR, origValue)
            }
        }
    }

    @Test
    void testInitStringPrefixResolution() throws IOException {
        final String origValue = System.getProperty(CloverNames.PROP_INITSTRING_PREFIX)
        try {
            final File tempDir1 = FileUtils.createTempDir("foo")
            System.setProperty(CloverNames.PROP_INITSTRING_PREFIX, tempDir1.getAbsolutePath() + File.separator)

            assertEquals(
                new File(tempDir1, "foo" + File.separator + "bar.db").getAbsolutePath(),
                Clover.resolveRegistryFile("foo" + File.separator + "bar.db", CloverProperties.newEmptyProperties()).getAbsolutePath())

            //Test dir//file -> dir/file
            assertEquals(
                new File(tempDir1, "foo" + File.separator + "bar.db").getAbsolutePath(),
                Clover.resolveRegistryFile(File.separator + "foo" + File.separator + "bar.db", CloverProperties.newEmptyProperties()).getAbsolutePath())
        } finally {
            if (origValue == null) {
                System.clearProperty(CloverNames.PROP_INITSTRING_PREFIX)
            } else {
                System.setProperty(CloverNames.PROP_INITSTRING_PREFIX, origValue)
            }
        }
    }

    @Test
    void testGetRecordingName() {
        // positive values
        String positive = Clover.getRecordingName(1000000, "clover.db", 1000000000)
        assertEquals("clover.dblfls_gjdgxs", positive)
        // negative values
        String negative = Clover.getRecordingName(-1000000, "clover.db", -1000000000)
        assertEquals("clover.dblfls_gjdgxs", negative)
        // int/long min
        String rangeMin = Clover.getRecordingName(Integer.MIN_VALUE, "clover.db", Long.MIN_VALUE)
        assertEquals("clover.dbzik0zj_1y2p0ij32e8e7", rangeMin)
        // int/long max
        String rangeMax = Clover.getRecordingName(Integer.MAX_VALUE, "clover.db", Long.MAX_VALUE)
        assertEquals("clover.dbzik0zj_1y2p0ij32e8e7", rangeMax)
        // zeroes
        String zeroes = Clover.getRecordingName(0, "", 0)
        assertEquals("0_0", zeroes)
    }

    @Test
    void testGetSliceRecordingName() {
        // positive values
        String positive = Clover.getSliceRecordingName(100, 200, 300, 1000000, "clover.db", 1000000000)
        assertEquals("clover.db5hb39hoo_8c_lfls_gjdgxs.s", positive)
        // negative values
        String negative = Clover.getSliceRecordingName(-100, -200, -300, -1000000, "clover.db", -1000000000)
        assertEquals("clover.db5k_8c_lfls_gjdgxs.s", negative)
        // int/long min
        String rangeMin = Clover.getSliceRecordingName(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, "clover.db", Long.MIN_VALUE)
        assertEquals("clover.dbzik0zk_zik0zj_zik0zj_1y2p0ij32e8e7.s", rangeMin)
        // int/long max
        String rangeMax = Clover.getSliceRecordingName(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, "clover.db", Long.MAX_VALUE)
        assertEquals("clover.db1y2p0ii3ju7en_zik0zj_zik0zj_1y2p0ij32e8e7.s", rangeMax)
        // zeroes
        String zeroes = Clover.getSliceRecordingName(0, 0, 0, 0, "", 0)
        assertEquals("0_0_0_0.s", zeroes)
    }
}
