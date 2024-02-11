package org.openclover.core.util;

/**
 * Code to detect what version of Java we are running on. It does not test for unsupported versions.
 * Based on the JavaEnvUtils from Ant
 */
public class JavaEnvUtils {
    /** Version of currently running VM. */
    private static String javaVersion;

    public static final String JAVA_8 = "8";
    public static final String JAVA_9 = "9";
    public static final String JAVA_10 = "10";
    public static final String JAVA_11 = "11";
    public static final String JAVA_12 = "12";
    public static final String JAVA_13 = "13";
    public static final String JAVA_14 = "14";
    public static final String JAVA_15 = "15";
    public static final String JAVA_16 = "16";
    public static final String JAVA_17 = "17";

    static {
        try {
            javaVersion = JAVA_8;
            Class.forName("java.lang.StackWalker");
            javaVersion = JAVA_9;
            Class.forName("java.util.stream.Collectors").getMethod("toUnmodifiableList");
            javaVersion = JAVA_10;
            String.class.getMethod("strip");
            javaVersion = JAVA_11;
            Class.class.getMethod("arrayType");
            javaVersion = JAVA_12;
            String.class.getMethod("translateEscapes");
            javaVersion = JAVA_13;
            Class.forName("java.io.Serial");
            javaVersion = JAVA_14;
            Class.class.getMethod("isHidden");
            javaVersion = JAVA_15;
            Class.forName("java.util.stream.Stream").getMethod("toList");
            javaVersion = JAVA_16;
            Class.forName("java.util.HexFormat");
            javaVersion = JAVA_17;
        } catch (Throwable t) {
            // ignore
        }
    }

    public static String getJavaVersion() {
        return javaVersion;
    }

    public static boolean isAtLeastJavaVersion(String expectedVersion) {
        return Integer.parseInt(javaVersion) >= Integer.parseInt(expectedVersion);
    }

    public static boolean isAtMostJavaVersion(String expectedVersion) {
        return Integer.parseInt(javaVersion) <= Integer.parseInt(expectedVersion);
    }
}
