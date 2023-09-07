package com.atlassian.clover.idea.actions.excludes;

import junit.framework.TestCase;

public class ExclusionUtilTest extends TestCase {

    String[][] testSets = {
            {"", "some/pattern/**", ""},
            {"other/pattern/**", "some/pattern/**", "other/pattern/**"},
            {"some/pattern/**", "some/pattern/**", ""},
            {"some/pattern/**, other/pattern", "some/pattern/**", "other/pattern"},
            {"other/pattern, some/pattern/**", "some/pattern/**", "other/pattern"},
            {"other/pattern, some/pattern/**, other/pattern1", "some/pattern/**", "other/pattern, other/pattern1"},
            {"other/pattern, other/pattern1", "some/pattern/**", "other/pattern, other/pattern1"},

    };

    public void testRemovePattern() {
        for (String[] testSet : testSets) {
            final String in = testSet[0];
            final String toBeRemoved = testSet[1];
            final String out = testSet[2];

            assertEquals("[" + in + "] - [" + toBeRemoved + "] = [" + out + "]", out, ExclusionUtil.removePattern(in, toBeRemoved));
        }
    }
    String[][] testSets2 = {
            {null, "some/pattern/**", "false"},
            {"", "some/pattern/**", "false"},
            {"other/pattern/**", "some/pattern/**", "false"},
            {"some/pattern/**", "some/pattern/**", "true"},
            {"some/pattern/**, other/pattern", "some/pattern/**", "true"},
            {"some/pattern/**", "some/other/pattern/**", "false"},
            {"other/pattern, some/pattern/**", "some/pattern/**", "true"},
            {"other/pattern, some/pattern/**, other/pattern1", "some/pattern/**", "true"},
            {"other/pattern, other/pattern1", "some/pattern/**", "false"},

    };

    public void testIsExplicitlyIncluded() {
        for (String[] testSet : testSets2) {
            final String config = testSet[0];
            final String pattern = testSet[1];
            final boolean included = Boolean.parseBoolean(testSet[2]);

            assertEquals("" + config + (included ? " includes " : " doesnt include ") + pattern, included, ExclusionUtil.isExplicitlyIncluded(config, pattern));
        }

    }

    String[][] testSetDisplayName = {
            {"com/cenqua/", "com/cenqua/"},
            {"com/cenqua/*.java", "com/cenqua/"},
            {"com/cenqua/**", "com/cenqua/"},
            {"com/cenqua/File.java", "com/cenqua/File.java"},
            {"", "<default>"},
            {"**", "<default>"},
            {"*.java", "<default>"},
    };


    public void testDisplayName() {
        for (String[] testSet : testSetDisplayName) {
           assertEquals(testSet[1], ExclusionUtil.getDisplayName(testSet[0]));
        }
    }
}
