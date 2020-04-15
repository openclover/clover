package com.atlassian.clover.idea.build;

import com.intellij.pom.java.LanguageLevel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * CloverCompiler Tester.
 */
public class CloverCompilerIdeaTest {

    @Test
    public void testLanguageLevelString() {
        assertNull(CloverCompiler.sourceLevelString(LanguageLevel.JDK_X));
        assertEquals("1.9", CloverCompiler.sourceLevelString(LanguageLevel.JDK_1_9));
        assertEquals("1.8", CloverCompiler.sourceLevelString(LanguageLevel.JDK_1_8));
        assertEquals("1.7", CloverCompiler.sourceLevelString(LanguageLevel.JDK_1_7));
        assertEquals("1.6", CloverCompiler.sourceLevelString(LanguageLevel.JDK_1_6));
        assertEquals("1.5", CloverCompiler.sourceLevelString(LanguageLevel.JDK_1_5));
        assertEquals("1.4", CloverCompiler.sourceLevelString(LanguageLevel.JDK_1_4));
        assertEquals("1.3", CloverCompiler.sourceLevelString(LanguageLevel.JDK_1_3));
    }

}
