package com.atlassian.clover.cfg.instr.java;

import clover.com.google.common.collect.Sets;
import com.atlassian.clover.util.JavaEnvUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Java language level of sources being instrumented.
 */
public enum SourceLevel {
    JAVA_7("1.7", Sets.newHashSet("1.7", "7"), Collections.<LanguageFeature>emptySet()),
    JAVA_8("1.8", Sets.newHashSet("1.8", "8"), Collections.singleton(LanguageFeature.LAMBDA)),
    JAVA_9("9", Sets.newHashSet("1.9", "9"), Sets.newHashSet(LanguageFeature.LAMBDA, LanguageFeature.MODULES)),
    JAVA_10("10", Sets.newHashSet("1.10", "10"), Sets.newHashSet(LanguageFeature.LAMBDA, LanguageFeature.MODULES)),
    JAVA_11("11", Sets.newHashSet("1.11", "11"), Sets.newHashSet(LanguageFeature.LAMBDA, LanguageFeature.MODULES));

    private static final Set<String> unsupportedSourceLevels =
            Sets.newHashSet("1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "5", "1.6", "6");
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
        if (JAVA_11.matchesVersion(source)) {
            return JAVA_11;
        }
        if (JAVA_10.matchesVersion(source)) {
            return JAVA_10;
        }
        if (JAVA_9.matchesVersion(source)) {
            return JAVA_9;
        }
        if (JAVA_8.matchesVersion(source)) {
            return JAVA_8;
        }
        return JAVA_7;
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
                source, com.atlassian.clover.cfg.instr.java.SourceLevel.JAVA_7.asString());
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
