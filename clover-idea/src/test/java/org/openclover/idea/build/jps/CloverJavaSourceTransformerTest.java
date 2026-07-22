package org.openclover.idea.build.jps;

import org.jetbrains.jps.model.java.LanguageLevel;
import org.junit.Test;
import org.openclover.core.cfg.instr.java.SourceLevel;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Java language level to Clover source level mapping used by the external (JPS) build.
 * This mapping used to have a twin in the in-process CloverCompiler; that copy was removed together
 * with the retired {@code JavaSourceTransformingCompiler} path, leaving this one as the single source of truth.
 */
public class CloverJavaSourceTransformerTest {

    private final CloverJavaSourceTransformer transformer = new CloverJavaSourceTransformer();

    @Test
    public void testLanguageLevelString() {
        // Java 7 and below map to the lowest source level Clover supports (Java 8)
        assertEquals(SourceLevel.JAVA_8, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_1_3));
        assertEquals(SourceLevel.JAVA_8, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_1_4));
        assertEquals(SourceLevel.JAVA_8, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_1_5));
        assertEquals(SourceLevel.JAVA_8, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_1_6));
        assertEquals(SourceLevel.JAVA_8, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_1_7));
        assertEquals(SourceLevel.JAVA_8, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_1_8));

        // Java 9 through 21 map one-to-one
        assertEquals(SourceLevel.JAVA_9, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_1_9));
        assertEquals(SourceLevel.JAVA_10, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_10));
        assertEquals(SourceLevel.JAVA_11, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_11));
        assertEquals(SourceLevel.JAVA_12, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_12));
        assertEquals(SourceLevel.JAVA_13, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_13));
        assertEquals(SourceLevel.JAVA_14, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_14));
        assertEquals(SourceLevel.JAVA_15, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_15));
        assertEquals(SourceLevel.JAVA_16, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_16));
        assertEquals(SourceLevel.JAVA_17, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_17));
        assertEquals(SourceLevel.JAVA_18, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_18));
        assertEquals(SourceLevel.JAVA_19, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_19));
        assertEquals(SourceLevel.JAVA_20, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_20));
        assertEquals(SourceLevel.JAVA_21, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_21));

        // Java 22-25 map one-to-one, but only on IntelliJ SDKs that actually define these constants
        // (older SDKs such as 2024.3 lack JDK_24/JDK_25); resolve reflectively and skip when absent.
        assertMappingIfPresent("JDK_22", SourceLevel.JAVA_22);
        assertMappingIfPresent("JDK_23", SourceLevel.JAVA_23);
        assertMappingIfPresent("JDK_24", SourceLevel.JAVA_24);
        assertMappingIfPresent("JDK_25", SourceLevel.JAVA_25);

        // levels newer than Clover's highest supported (Java 25) and the open-ended JDK_X
        // fall back to Java 25
        assertEquals(SourceLevel.JAVA_25, transformer.languageLevelToSourceLevel(LanguageLevel.JDK_X));
    }

    private void assertMappingIfPresent(final String languageLevelName, final SourceLevel expected) {
        final LanguageLevel level;
        try {
            level = LanguageLevel.valueOf(languageLevelName);
        } catch (IllegalArgumentException notInThisSdk) {
            // this IntelliJ SDK predates the given JDK language level - nothing to assert
            return;
        }
        assertEquals(expected, transformer.languageLevelToSourceLevel(level));
    }
}
