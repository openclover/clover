package com.atlassian.clover.util;

import java.util.stream.Collectors;

/**
 * Code to detect what version of Java we are running on. It does not test for unsupported versions.
 * Based on the JavaEnvUtils from Ant
 */
public class JavaEnvUtils {
    /** Version of currently running VM. */
    private static String javaVersion;

    private static final String JAVA_7 = "7";
    private static final String JAVA_8 = "8";
    private static final String JAVA_9 = "9";
    private static final String JAVA_10 = "10";
    private static final String JAVA_11 = "11";

    static {
        try {
            javaVersion = JAVA_7;
            Class.forName("java.lang.reflect.Executable");
            javaVersion = JAVA_8;
            Class.forName("java.lang.StackWalker");
            javaVersion = JAVA_9;
            Collectors.class.getMethod("toUnmodifiableList");
            javaVersion = JAVA_10;
            String.class.getMethod("strip");
            javaVersion = JAVA_11;
        } catch (Throwable t) {
            // ignore
        }
    }

    public static String getJavaVersion() {
        return javaVersion;
    }
}
