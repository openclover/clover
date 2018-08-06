package com.atlassian.clover.cfg.instr.java;

import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.util.JavaEnvUtils;

/**
 * Supplementary instrumentation settings related with Java.
 */
public class JavaInstrumentationConfig extends InstrumentationConfig {
    public static final String JAVA_LANG_PREFIX = "java.lang.";

    /** true if we are dealing explicitly with java 1.4 source **/
    private boolean java14 = false;
    /** true if we are dealing explicitly with java 1.5 source **/
    private boolean java15 = false;
    /** true if we are dealing explicitly with java 1.6 source **/
    private boolean java16 = false;
    /** true if we are dealing explicitly with java 1.7 source **/
    private boolean java17 = false;
    /** true if we are dealing explicitly with java 8 source **/
    private boolean java8 = false;
    /** true if we are dealing explicitly with java 9 source **/
    private boolean java9 = false;

    private boolean sourceLevelSet = false;

    /** if true, use fully qualified names for Java vars **/
    private boolean fullyQualifiedJavaNames = true;

    private String instrFileExtension = "java";

    private LambdaInstrumentation instrumentLambda = LambdaInstrumentation.NONE;

    public String getJavaLangPrefix() {
        return fullyQualifiedJavaNames ? JAVA_LANG_PREFIX : "";
    }

    public void setFullyQualifyJavaLang(boolean fullyQualifiedJavaNames) {
        this.fullyQualifiedJavaNames = fullyQualifiedJavaNames;
    }

    public String getSourceLevel() {
        ensureSourceLevelSet();
        return  java9 ? "9"
                : java8 ? "1.8"
                    : java17 ? "1.7"
                        : java16 ? "1.6"
                            : java15 ? "1.5"
                                : java14 ? "1.4"
                                    : "1.3";
    }

    public void setSourceLevel(String source) {
        if (source != null) {
            java9 = source.equals("1.9") || source.equals("9");
            java8 = source.equals("1.8") || source.equals("8") || java9;
            java17 = source.equals("1.7") || source.equals("7") || java8;
            java16 = source.equals("1.6") || source.equals("6") || java17;
            java15 = source.equals("1.5") || source.equals("5") || java16;
            java14 = source.equals("1.4") || java15;
            sourceLevelSet = true;
        }
    }

    public boolean isJava14() {
        ensureSourceLevelSet();
        return java14;
    }

    public boolean isJava15() {
        ensureSourceLevelSet();
        return java15;
    }

    public boolean isJava8() {
        ensureSourceLevelSet();
        return java8;
    }

    public boolean isJava9(){
        ensureSourceLevelSet();
        return java9;
    }

    public void setInstrFileExtension(String extension) {
        instrFileExtension = extension;
    }

    public String getInstrFileExtension() {
        return instrFileExtension;
    }

    public void setInstrumentLambda(LambdaInstrumentation instrumentLambda) {
        this.instrumentLambda = instrumentLambda;
    }

    public LambdaInstrumentation getInstrumentLambda() {
        return instrumentLambda;
    }

    protected String determineSourceLevel() {
        return JavaEnvUtils.getJavaVersion();
    }

    private void ensureSourceLevelSet() {
        if (!sourceLevelSet) {
            setSourceLevel(determineSourceLevel());
        }
    }
}
