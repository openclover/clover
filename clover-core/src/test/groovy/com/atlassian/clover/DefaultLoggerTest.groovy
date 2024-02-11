package com.atlassian.clover

import junit.framework.TestCase
import org.openclover.runtime.DefaultLogger

class DefaultLoggerTest extends TestCase {

    final PrintStream defaultErr = System.err
    final PrintStream defaultOut = System.out
    ByteArrayOutputStream out = new ByteArrayOutputStream()
    ByteArrayOutputStream err = new ByteArrayOutputStream()

    private static String ERROR_PREFIX = "ERROR: "

    boolean originalDebug
    boolean originalVerbose

    void setUp() {
        System.setOut(new PrintStream(out))
        System.setErr(new PrintStream(err))
        originalDebug = DefaultLogger.isDebug()
        originalVerbose = DefaultLogger.isVerbose()
    }

    void tearDown() {
        System.setOut(defaultOut)
        System.setErr(defaultErr)
        DefaultLogger.setDebug(originalDebug)
        DefaultLogger.setVerbose(originalVerbose)
    }

    void testLogInfo() throws IOException {
        DefaultLogger log = new DefaultLogger()
        DefaultLogger.setDebug(false)
        DefaultLogger.setVerbose(false)
        final String msg = "Testing"
        log.info(msg)
        assertSystemOutEquals(msg)
        assertEquals(0, err.size())

        log.info(msg, new Exception(msg + " exception"))
        assertSystemOutEquals(msg)
        assertEquals(0, err.size())
    }

    void testLogError() throws IOException {
        DefaultLogger log = new DefaultLogger()
        DefaultLogger.setDebug(false)
        DefaultLogger.setVerbose(false)
        final String msg = "Testing"
        log.error(msg)
        assertSystemErrEquals(ERROR_PREFIX + msg)
        assertEquals(0, out.size()); // ensure nothing written to sysout

        log.error(msg, new Exception(msg + " exception"))
        assertSystemErrEquals(ERROR_PREFIX + msg)
        assertEquals(0, out.size())
    }


    void testLogVerbose() throws IOException {
        DefaultLogger log = new DefaultLogger()
        DefaultLogger.setDebug(true)
        
        final String msg = "Testing"
        log.verbose(msg)
        assertSystemOutEquals(msg)
        assertEquals(0, err.size())

        final String exceptionMessage = msg + " exception"
        log.verbose(msg, new Exception(exceptionMessage))
        assertLineEquals(msg + " : " + exceptionMessage, err)
        assertLineEquals(msg, out)
    }

    void testCanIgnore() {
        DefaultLogger.setDebug(false)
        DefaultLogger log = new DefaultLogger()
        log.debug("Should be ignored")
        assertEquals(0, out.size())
        assertEquals(0, err.size())

    }

    private void assertSystemOutEquals(String msg) throws IOException {
        assertStreamEqualsAndReset(msg, out)
    }

    private void assertSystemErrEquals(String msg) throws IOException {
        assertStreamEqualsAndReset(msg, err)
    }

    private void assertStreamEqualsAndReset(String msg, ByteArrayOutputStream stream) throws IOException {
        BufferedReader reader = assertLineEquals(msg, stream)
        assertStreamEmpty(stream, reader)
    }

    private static BufferedReader assertLineEquals(String msg, ByteArrayOutputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(stream.toByteArray())))
        final String loggedMessage = reader.readLine()
        assertEquals(msg, loggedMessage)
        return reader
    }

    private static void assertStreamEmpty(ByteArrayOutputStream stream, BufferedReader reader) throws IOException {
        assertEquals(-1, reader.read())
        stream.reset()
    }
}
