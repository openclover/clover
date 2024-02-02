package org.openclover.idea.util;

import junit.framework.TestCase;

/**
 * BuildUtil Tester.
 */
public class InclusionUtilTest extends TestCase {
    public void testIncludes() {
        String[] includes = {
                "**/*Part.*",
                "**/*Part.java",
                "root/sub/sub1/*.*",
                "*/sub/sub1/*.*",
                "**/sub1/*.*",
                "**/Start*.*",

                "**\\*Part.*",
                "**\\*Part.java",
                "root\\sub\\sub1\\*.*",
                "*\\sub\\sub1\\*.*",
                "**\\sub1\\*.*",
                "**\\Start*.*"

        };
        String[] matchingSources = {
                "root/sub/sub1/StartPart.java",
                "root\\sub\\sub1\\StartPart.java",
        };

        for (String source : matchingSources) {
            for (String include : includes) {
                final String test = include + " -> " + source;
                assertTrue(test, InclusionUtil.included(source, InclusionUtil.toArray(include, " ,")));
                assertTrue(test, InclusionUtil.included(source, InclusionUtil.toArray(include + " ,nomatter", " ,")));
                assertTrue(test, InclusionUtil.included(source, InclusionUtil.toArray("nomatter, " + include, " ,")));
                assertTrue(test, InclusionUtil.included(source, InclusionUtil.toArray(include + " nomatter", " ,")));
                assertTrue(test, InclusionUtil.included(source, InclusionUtil.toArray("nomatter " + include, " ,")));
                assertTrue(test, InclusionUtil.included(source, InclusionUtil.toArray(include + ",nomatter", " ,")));
                assertTrue(test, InclusionUtil.included(source, InclusionUtil.toArray("nomatter," + include, " ,")));

                assertTrue(test, InclusionUtil.excluded(source, InclusionUtil.toArray(include, " ,")));
                assertTrue(test, InclusionUtil.excluded(source, InclusionUtil.toArray(include + " ,nomatter", " ,")));
                assertTrue(test, InclusionUtil.excluded(source, InclusionUtil.toArray("nomatter, " + include, " ,")));
                assertTrue(test, InclusionUtil.excluded(source, InclusionUtil.toArray(include + " nomatter", " ,")));
                assertTrue(test, InclusionUtil.excluded(source, InclusionUtil.toArray("nomatter " + include, " ,")));
                assertTrue(test, InclusionUtil.excluded(source, InclusionUtil.toArray(include + ",nomatter", " ,")));
                assertTrue(test, InclusionUtil.excluded(source, InclusionUtil.toArray("nomatter," + include, " ,")));

            }
        }


    }
}
