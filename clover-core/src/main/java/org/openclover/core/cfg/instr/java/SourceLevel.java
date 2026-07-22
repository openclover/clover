package org.openclover.core.cfg.instr.java;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.util.JavaEnvUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.openclover.core.cfg.instr.java.LanguageFeature.FLEXIBLE_CONSTRUCTORS;
import static org.openclover.core.cfg.instr.java.LanguageFeature.LAMBDA;
import static org.openclover.core.cfg.instr.java.LanguageFeature.MODULES;
import static org.openclover.core.cfg.instr.java.LanguageFeature.PATTERN_MATCHING;
import static org.openclover.core.cfg.instr.java.LanguageFeature.RECORDS;
import static org.openclover.core.cfg.instr.java.LanguageFeature.SWITCH_EXPRESSIONS;
import static org.openclover.core.cfg.instr.java.LanguageFeature.TEXT_BLOCKS;
import static org.openclover.core.util.Sets.newHashSet;

/**
 * Java language level of sources being instrumented.
 */
public enum SourceLevel {
    JAVA_8("1.8", newHashSet("1.8", "8"), Collections.singleton(LAMBDA)),
    JAVA_9("9", newHashSet("1.9", "9"), newHashSet(LAMBDA, MODULES)),
    JAVA_10("10", newHashSet("1.10", "10"), newHashSet(LAMBDA, MODULES)),
    JAVA_11("11", newHashSet("1.11", "11"), newHashSet(LAMBDA, MODULES)),
    JAVA_12("12", newHashSet("12"), newHashSet(LAMBDA, MODULES)),
    JAVA_13("13", newHashSet("13"), newHashSet(LAMBDA, MODULES)),
    JAVA_14("14", newHashSet("14"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS)),
    JAVA_15("15", newHashSet("15"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS)),
    JAVA_16("16", newHashSet("16"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS)),
    JAVA_17("17", newHashSet("17"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS)),
    JAVA_18("18", newHashSet("18"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS)),
    JAVA_19("19", newHashSet("19"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS)),
    JAVA_20("20", newHashSet("20"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS)),
    JAVA_21("21", newHashSet("21"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS, PATTERN_MATCHING)),
    JAVA_22("22", newHashSet("22"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS, PATTERN_MATCHING)),
    JAVA_23("23", newHashSet("23"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS, PATTERN_MATCHING)),
    JAVA_24("24", newHashSet("24"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS, PATTERN_MATCHING)),
    JAVA_25("25", newHashSet("25"), newHashSet(LAMBDA, MODULES, SWITCH_EXPRESSIONS, TEXT_BLOCKS, RECORDS, PATTERN_MATCHING, FLEXIBLE_CONSTRUCTORS));

    private static final Set<String> unsupportedSourceLevels =
            newHashSet("1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "5", "1.6", "6", "1.7", "7");
    private final String primaryVersionString;
    private final HashSet<String> versionStrings;
    private final Set<LanguageFeature> languageFeatures;

    SourceLevel(String primaryVersionString,
                HashSet<String> versionStrings,
                Set<LanguageFeature> languageFeatures) {
        this.primaryVersionString = primaryVersionString;
        this.versionStrings = versionStrings;
        this.languageFeatures = languageFeatures;
    }

    /**
     * Parse source level string and return an enum.
     *
     * @param source level
     * @return SourceLevel
     */
    public static SourceLevel fromString(@NotNull String source) {
        if (JAVA_25.matchesVersion(source)) {
            return JAVA_25;
        }
        if (JAVA_24.matchesVersion(source)) {
            return JAVA_24;
        }
        if (JAVA_23.matchesVersion(source)) {
            return JAVA_23;
        }
        if (JAVA_22.matchesVersion(source)) {
            return JAVA_22;
        }
        if (JAVA_21.matchesVersion(source)) {
            return JAVA_21;
        }
        if (JAVA_20.matchesVersion(source)) {
            return JAVA_20;
        }
        if (JAVA_19.matchesVersion(source)) {
            return JAVA_19;
        }
        if (JAVA_18.matchesVersion(source)) {
            return JAVA_18;
        }
        if (JAVA_17.matchesVersion(source)) {
            return JAVA_17;
        }
        if (JAVA_16.matchesVersion(source)) {
            return JAVA_16;
        }
        if (JAVA_15.matchesVersion(source)) {
            return JAVA_15;
        }
        if (JAVA_14.matchesVersion(source)) {
            return JAVA_14;
        }
        if (JAVA_13.matchesVersion(source)) {
            return JAVA_13;
        }
        if (JAVA_12.matchesVersion(source)) {
            return JAVA_12;
        }
        if (JAVA_11.matchesVersion(source)) {
            return JAVA_11;
        }
        if (JAVA_10.matchesVersion(source)) {
            return JAVA_10;
        }
        if (JAVA_9.matchesVersion(source)) {
            return JAVA_9;
        }
        return JAVA_8;
    }

    /**
     * Detect Java version used and return an enum.
     *
     * @return SourceLevel
     */
    public static SourceLevel autoDetect() {
        return fromString(JavaEnvUtils.getJavaVersion());
    }

    /**
     * Return true if given source level is no longer supported.
     * @param source level, e.g. "1.0"
     * @return boolean if supported
     */
    public static boolean isUnsupported(@NotNull String source) {
        return unsupportedSourceLevels.contains(source);
    }

    public static String getUnsupportedMessage(@NotNull String source) {
        return String.format("Source level '%s' is unsupported, assuming '%s'.",
                source, JAVA_8.asString());
    }

    public boolean supportsFeature(LanguageFeature feature) {
        return languageFeatures.contains(feature);
    }

    public boolean matchesVersion(String version) {
        return versionStrings.contains(version);
    }

    public String asString() {
        return primaryVersionString;
    }

}
