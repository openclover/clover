package org.openclover.core.util

import org.junit.Test

import static org.junit.Assert.assertEquals

class ChecksummingReaderTest {

    @Test
    void testChecksumNormalization() throws Exception {
        byte[] input1 = "multi\r\nline\ntest\r\n blah".getBytes("ASCII")
        byte[] input2 = "multi\nline\ntest\n blah".getBytes("ASCII")

        ChecksummingReader udr1 = new ChecksummingReader(
                new InputStreamReader(new ByteArrayInputStream(input1)))

        ChecksummingReader udr2 = new ChecksummingReader(
                new InputStreamReader(new ByteArrayInputStream(input2)))

        readAll(udr1)
        readAll(udr2)
        assertEquals(udr1.getChecksum(), udr2.getChecksum())
    }

    private static void readAll(Reader input) throws IOException {
        int c = 0
        while (c >= 0) {
            c = input.read()
        }
    }

}
