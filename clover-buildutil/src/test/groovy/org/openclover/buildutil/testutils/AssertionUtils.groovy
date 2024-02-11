package org.openclover.buildutil.testutils

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class AssertionUtils {

    /**
     * Check whether string contains substring.
     * @param expectedSubstring substring we look for
     * @param actualString      string to be searched
     * @param negate            <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertStringContains(String expectedSubstring, String actualString, boolean negate) {
        if (!negate) {
            assertTrue("A substring \n'" + expectedSubstring + "'\n was not found in \n'" + actualString + "'",
                    actualString.contains(expectedSubstring))
        } else {
            assertFalse("A substring \n'" + expectedSubstring + "'\n was found in \n'" + actualString + "'.",
                    actualString.contains(expectedSubstring))
        }
    }

    /**
     * Check whether string matches regular expression.
     * @param regExp         regular expression we look for
     * @param actualString   string to be searched
     * @param negate         <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertStringMatches(String regExp, String actualString, boolean negate) {
        final Pattern pattern = Pattern.compile(regExp)
        final Matcher matcher = pattern.matcher(actualString)
        if (!negate) {
            assertTrue("A pattern \n'" + regExp + "'\n was not found in \n'" + actualString + "'.",
                    matcher.find())
        } else {
            assertFalse("A pattern \n'" + regExp + "'\n was found in \n'" + actualString + "'.",
                    matcher.find())
        }
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

    /**
     * Check whether file content matches regular expression.
     * @param regExp         regular expression we look for
     * @param actualFile     file to be searched
     * @param negate         <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertFileMatches(String regExp, File actualFile, boolean negate) {
        final String fileContent = readFile(actualFile)
        final Pattern pattern = Pattern.compile(regExp)
        final Matcher matcher = pattern.matcher(fileContent)
        if (!negate) {
            assertTrue("A pattern '" + regExp + "' was not found in file '" + actualFile.getAbsolutePath() + "'.",
                    matcher.find())
        } else {
            assertFalse("A pattern '" + regExp + "' was found in file '" + actualFile.getAbsolutePath() + "'.",
                    matcher.find())
        }
    }

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

}
