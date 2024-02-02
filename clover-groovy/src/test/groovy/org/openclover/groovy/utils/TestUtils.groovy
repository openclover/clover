package org.openclover.groovy.utils

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class TestUtils {

    /**
     * Read file contents into a String.
     * @param inputFile file to be read
     * @return String - file content
     */
    private static String readFile(final File inputFile) {
        final int BUF_SIZE = 8000
        final char[] buffer = new char[BUF_SIZE]
        final StringBuilder out = new StringBuilder()

        try {
            int charsRead
            final Reader fileReader = new BufferedReader(new FileReader(inputFile))

            while ( (charsRead = fileReader.read(buffer, 0, BUF_SIZE)) != -1 ) {
                out.append(buffer, 0, charsRead)
            }
            fileReader.close()
        } catch (IOException ex) {
            fail(ex.toString())
        }

        out.toString()
    }

    /**
     * Check whether file content contains substring.
     * @param substring         substring we look for
     * @param actualFile        file to be searched
     * @param negate            <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertFileContains(String substring, File actualFile, boolean negate) {
        final String fileContent = readFile(actualFile)
        if (!negate) {
            assertTrue("A substring '" + substring + "' was not found in file '" + actualFile.getAbsolutePath() + "'.",
                    fileContent.contains(substring))
        } else {
            assertFalse("A substring '" + substring + "' was found in file '" + actualFile.getAbsolutePath() + "'.",
                    fileContent.contains(substring))
        }
    }

}
