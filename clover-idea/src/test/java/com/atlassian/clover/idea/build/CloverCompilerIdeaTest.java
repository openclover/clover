package com.atlassian.clover.idea.build;

import com.atlassian.clover.cfg.instr.java.SourceLevel;
import com.intellij.pom.java.LanguageLevel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * CloverCompiler Tester.
 */
public class CloverCompilerIdeaTest {

    @Test
    public void testLanguageLevelString() {
        assertEquals(SourceLevel.JAVA_9, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_X));
        assertEquals(SourceLevel.JAVA_9, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_1_9));
        assertEquals(SourceLevel.JAVA_8, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_1_8));
        assertEquals(SourceLevel.JAVA_7, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_1_7));
        assertEquals(SourceLevel.JAVA_7, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_1_6));
        assertEquals(SourceLevel.JAVA_7, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_1_5));
        assertEquals(SourceLevel.JAVA_7, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_1_4));
        assertEquals(SourceLevel.JAVA_7, CloverCompiler.languageLevelToSourceLevel(LanguageLevel.JDK_1_3));
    }

}
