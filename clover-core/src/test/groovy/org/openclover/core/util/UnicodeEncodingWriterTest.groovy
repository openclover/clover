package org.openclover.core.util

import org.junit.Test

import static org.junit.Assert.assertEquals

class UnicodeEncodingWriterTest {

    @Test
    void testEncoding() throws Exception {
        verifyString("sanity")

        //regression values from XOM project:
        verifyStringIgnoreCase("\u0245", "\\u0245")
        verifyStringIgnoreCase("\uD8F5\uDF80", "\\uD8F5\\uDF80")
        verifyStringIgnoreCase("\uD8F5\uDBF0","\\uD8F5\\uDBF0")
        verifyStringIgnoreCase("\uD8F5\uDBF5","\\uD8F5\\uDBF5")
        verifyStringIgnoreCase("\uD8F5","\\uD8F5")
        verifyStringIgnoreCase("\uD8F0","\\uD8F0")
    }

    private void verifyString(String input) throws IOException {
        assertEquals(input, encodeString(input))
    }

    private void verifyStringIgnoreCase(String input, String expected) throws IOException {
        assertEquals(expected.toUpperCase(Locale.ENGLISH), encodeString(input).toUpperCase(Locale.ENGLISH))
    }

    private String encodeString(String input) throws IOException {
        StringWriter swout = new StringWriter()
        Writer out = new UnicodeEncodingWriter(swout)
        out.write(input)
        out.close()
        swout.toString()
    }
}
