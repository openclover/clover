package com.atlassian.clover.idea.util;

import junit.framework.TestCase;

/**
 * BlameClover Tester.
 */
public class BlameCloverTest extends TestCase {


    public void testGetBugWithDescriptionUrl() throws Exception {
        BlameClover blameClover = new BlameCloverTestFixture();
        String s = blameClover.getBugWithDescriptionUrl("DESCRIPTION with spaces, enters\nand ?\n");
        String descrPart = s.substring(s.indexOf("&description=") + "&description=".length());
        assertEquals("DESCRIPTION+with+spaces%2C+enters%0Aand+%3F%0A", descrPart);

    }

    private static final Object[][] EXPECTED = {
            {"description1", 0, ""},
            {"description2", 10, ""},
            {"description3", 100, "description3"},
            {"description4", 12, "description4"},
            {"description5\ndescription", 0, ""},
            {"description6\ndescription", 13, "description6"},
            {"description7\ndescription", 15, "description7"},
            {"description8\ndescription", 100, "description8%0Adescription"},
            {"d    \nx    \ndescription", 13, "d++++%0Ax++++"},
            {"d++++\nx++++\ndescription", 13, "d%2B%2B%2B%2B"},
            {"d    \nx    \ndescription", 3, ""},
            {"d++++\nx++++\ndescription", 3, ""},


    };

    public void testGetBoundedEncodedString() throws Exception {
        BlameClover blameClover = new BlameCloverTestFixture();
        for (int i = 0; i < EXPECTED.length; i++) {
            Object[] testcase = EXPECTED[i];
            assertEquals("Test " + i, testcase[2], blameClover.getBoundedEncodedString((String) testcase[0], (Integer) testcase[1]));
        }

    }

}

class BlameCloverTestFixture extends BlameClover {
    @Override
    protected String getIdeaBuild() {
        return "X.Y.Z";
    }
}