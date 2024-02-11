package org.openclover.core.util

import org.openclover.core.ProgressListener
import junit.framework.TestCase
import org.openclover.core.TestUtils

class ProgressFileInputStreamTest extends TestCase {

    private File testFile
    private static final double DELTA = 0.0001

    protected void setUp() throws Exception {
        testFile = File.createTempFile("clovertest", ".tmp", TestUtils.createEmptyDirFor(getClass(), getName()))
        testFile.deleteOnExit()
    }

    protected void tearDown() throws Exception {
        testFile.delete()
    }

    void testEmpty() throws Exception {
        final ProgressListenerImpl progress = new ProgressListenerImpl()

        final ProgressInputStream pfis = new ProgressInputStream(new FileInputStream(testFile), testFile.length(), progress, "msg")
        assertEquals(-1, pfis.read())
        assertEquals(-1, pfis.read(new byte[1]))
        assertEquals(-1, pfis.read(new byte[32], 16, 1))
    }

    void testRead() throws Exception {
        final ProgressListenerImpl progress = new ProgressListenerImpl()

        fillFile(100)

        final ProgressInputStream pfis = new ProgressInputStream(new FileInputStream(testFile), testFile.length(), progress, "msg")
        pfis.read()
        final int r1 = pfis.read(new byte[9])
        assertEquals(9, r1)
        assertEquals("msg", progress.lastMsg)
        assertEquals(0.1f, progress.lastPc, DELTA)
        final int r2 = pfis.read(new byte[10])
        assertEquals(10, r2)
        assertEquals("msg", progress.lastMsg)
        assertEquals(0.2f, progress.lastPc, DELTA)

        for (int skip = 20; skip > 0; skip -= pfis.skip(skip)) {
            // nothing
        }
        assertEquals(0.4f, progress.lastPc, DELTA)

        final int r3 = pfis.read(new byte[20], 10, 10)
        assertEquals(10, r3)
        assertEquals("msg", progress.lastMsg)
        assertEquals(0.5f, progress.lastPc, DELTA)
    }

    void testCancel() throws Exception {
        fillFile(100)

        final ProgressListener progress = new AbortingProgressListenerImpl()
        final ProgressInputStream pfis = new ProgressInputStream(new FileInputStream(testFile), testFile.length(), progress, "msg")

        Exception thrown = null
        try {
            pfis.read(new byte[10])
        } catch (RuntimeException e) {
            thrown = e
        }

        assertTrue(thrown instanceof ProgressCancelledException)
        thrown = null

        try {
            pfis.read(new byte[10], 1, 1)
        } catch (RuntimeException e) {
            thrown = e
        }

        assertTrue(thrown instanceof ProgressCancelledException)
        thrown = null

        try {
            pfis.skip(10)
        } catch (RuntimeException e) {
            thrown = e
        }

        assertTrue(thrown instanceof ProgressCancelledException)
    }

    void testMark() throws Exception {
        fillFile(100)
        final ProgressListenerImpl progress = new ProgressListenerImpl()
        final ProgressInputStream pfis = new ProgressInputStream(new BufferedInputStream(new FileInputStream(testFile)), testFile.length(), progress, "msg")

        pfis.read()
        final int r1 = pfis.read(new byte[9])
        assertEquals(9, r1)
        assertEquals("msg", progress.lastMsg)
        assertEquals(0.1f, progress.lastPc, DELTA)

        pfis.mark(100)
        final int r2 = pfis.read(new byte[10])
        assertEquals(10, r2)
        assertEquals("msg", progress.lastMsg)
        assertEquals(0.2f, progress.lastPc, DELTA)

        pfis.reset()
        assertEquals("msg", progress.lastMsg)
        assertEquals(0.1f, progress.lastPc, DELTA)

        final int r3 = pfis.read(new byte[10])
        assertEquals(10, r3)
        assertEquals("msg", progress.lastMsg)
        assertEquals(0.2f, progress.lastPc, DELTA)


    }

    private void fillFile(int len) throws IOException {
        final FileOutputStream fos = new FileOutputStream(testFile)
        fos.write(new byte[len])
        fos.close()
    }

    private static class ProgressListenerImpl implements ProgressListener {
        String lastMsg
        float lastPc

        void handleProgress(String desc, float pc) {
            lastMsg = desc
            lastPc = pc
        }
    }

    private static class ProgressCancelledException extends RuntimeException {
    }

    private static class AbortingProgressListenerImpl implements ProgressListener {

        void handleProgress(String desc, float pc) {
            throw new ProgressCancelledException()
        }
    }

}
