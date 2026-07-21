package org.openclover.core.cfg.instr.java

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Unit tests for {@link SourceLevel} version parsing and feature sets, focusing on the
 * Java 18-21 levels added for OC-229.
 */
class SourceLevelTest {

    @Test
    void testFromStringResolvesJava18To21() {
        assertEquals(SourceLevel.JAVA_18, SourceLevel.fromString("18"))
        assertEquals(SourceLevel.JAVA_19, SourceLevel.fromString("19"))
        assertEquals(SourceLevel.JAVA_20, SourceLevel.fromString("20"))
        assertEquals(SourceLevel.JAVA_21, SourceLevel.fromString("21"))
    }

    @Test
    void testFromStringStillResolvesEarlierLevels() {
        assertEquals(SourceLevel.JAVA_17, SourceLevel.fromString("17"))
        assertEquals(SourceLevel.JAVA_8, SourceLevel.fromString("1.8"))
        assertEquals(SourceLevel.JAVA_8, SourceLevel.fromString("8"))
    }

    @Test
    void testUnknownNewerLevelFallsBackToJava8() {
        // fromString keeps the historical behaviour of defaulting to Java 8 for anything unmatched
        assertEquals(SourceLevel.JAVA_8, SourceLevel.fromString("99"))
    }

    @Test
    void testPatternMatchingFeatureIsJava21Only() {
        assertFalse(SourceLevel.JAVA_17.supportsFeature(LanguageFeature.PATTERN_MATCHING))
        assertFalse(SourceLevel.JAVA_20.supportsFeature(LanguageFeature.PATTERN_MATCHING))
        assertTrue(SourceLevel.JAVA_21.supportsFeature(LanguageFeature.PATTERN_MATCHING))
    }

    @Test
    void testJava21RetainsEarlierFeatures() {
        assertTrue(SourceLevel.JAVA_21.supportsFeature(LanguageFeature.RECORDS))
        assertTrue(SourceLevel.JAVA_21.supportsFeature(LanguageFeature.SWITCH_EXPRESSIONS))
        assertTrue(SourceLevel.JAVA_21.supportsFeature(LanguageFeature.TEXT_BLOCKS))
    }
}
