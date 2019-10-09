package com.atlassian.clover.instr.java;

import static com.atlassian.clover.instr.Bindings.$Clover$getNullRecorder;
import static com.atlassian.clover.instr.Bindings.$Clover$getRecorder;
import static com.atlassian.clover.instr.Bindings.$Clover$l;
import static com.atlassian.clover.instr.Bindings.$CloverVersionInfo$getBuildStamp;
import static com.atlassian.clover.instr.Bindings.$CloverVersionInfo$getReleaseNum;
import static com.atlassian.clover.instr.Bindings.$CloverVersionInfo$oldVersionInClasspath;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.recorder.pertest.SnifferType;
import com_atlassian_clover.TestNameSniffer;
import com_atlassian_clover.CloverProfile;
import com_atlassian_clover.Clover;
import com_atlassian_clover.CloverVersionInfo;
import com_atlassian_clover.CoverageRecorder;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class RecorderInstrEmitter extends Emitter {

    static final String LAMBDA_INC_METHOD = "lambdaInc";

    private static final String INCOMPATIBLE_MSG =
            "[CLOVER] WARNING: The Clover version used in instrumentation does " +
                    "not match the runtime version. You need to run instrumented classes against the same version of Clover " +
                    "that you instrumented with.";
    private static final String DEFAULT_CLASSNOTFOUND_MSG =
            "[CLOVER] FATAL ERROR: Clover could not be initialised. Are you " +
                    "sure you have Clover in the runtime classpath?";
    private static final String UNEXPECTED_MSG =
            "[CLOVER] FATAL ERROR: Clover could not be initialised because of an unexpected error.";

    private boolean isEnum;
    private boolean reportInitErrors;
    private boolean classInstrStrategy;
    private String recorderPrefix;
    private long recorderCfg;
    private String initString;
    private long registryVersion;
    private int maxDataIndex;
    private String javaLangPrefix;
    private boolean testClass;
    /**
     * Whether it's a Spock framework test class
     */
    private boolean isSpockTestClass;
    /**
     * Whether it's a JUnit parameterized test class
     */
    private boolean isParameterizedJUnitTestClass;

    /**
     * To get a lazy evaluation whether it's a JUnit 5 parameterized test class. This is because at the time when
     * RecorderInstrEmitter is initialized (beginning of the class), we stil do not know this (only after we detect
     * a parameterized method).
     */
    private class InstrumentationStateView {
        private InstrumentationState state;

        private InstrumentationStateView(InstrumentationState state) {
            this.state = state;
        }

        private boolean isJUnit5ParameterizedTest() {
            return state.isParameterizedJUnit5TestClass();
        }
    }
    private InstrumentationStateView stateView;


    private String distributedConfig;
    private String classNotFoundMsg;
    private boolean shouldEmitWarningMethod;
    private List<CloverProfile> profiles;
    private boolean isJava8OrHigher;

    public RecorderInstrEmitter(boolean isEnum) {
        super();
        this.isEnum = isEnum;
    }

    @Override
    public void init(InstrumentationState state) {
        // save these configs as instance variables, in case state changes between now and exec().
        recorderCfg = getConfigBits(state.getCfg());
        recorderPrefix = state.getRecorderPrefix();
        classInstrStrategy = state.getCfg().isClassInstrStrategy();
        reportInitErrors = state.getCfg().isReportInitErrors();
        initString = state.getCfg().getInitString();
        distributedConfig = state.getCfg().getDistributedConfigString();
        profiles = state.getCfg().getProfiles();
        registryVersion = state.getSession().getVersion();
        javaLangPrefix = state.getCfg().getJavaLangPrefix();
        isJava8OrHigher = state.getCfg().isJava8();
        testClass = state.isDetectTests();
        isSpockTestClass = state.isSpockTestClass();
        isParameterizedJUnitTestClass = state.isParameterizedJUnitTestClass();
        classNotFoundMsg = state.getCfg().getClassNotFoundMsg() != null ? state.getCfg().getClassNotFoundMsg() : DEFAULT_CLASSNOTFOUND_MSG;
        //Only emit the warning method in instr code once per instr session. This minimises permgen pressure.
        shouldEmitWarningMethod = !state.hasInstrumented();
        if (!state.hasInstrumented()) {
            state.setHasInstrumented(true);
        }

        // for lazy eval
        stateView = new InstrumentationStateView(state);
    }

    @Override
    public String getInstr() {
        String instrString;
        if (classInstrStrategy || isEnum) {
            String recorderBase = recorderPrefix.substring(0, recorderPrefix.lastIndexOf('.'));
            String recorderSuffix = recorderPrefix.substring(recorderPrefix.lastIndexOf('.') + 1);

            // public static class __CLR3_1_600hckkb3w8 {
            instrString = (testClass ? "" : "public ") + "static class " + recorderBase + "{";
            // public static com_atlassian_clover.CoverageRecorder R;
            instrString += "public static " + CoverageRecorder.class.getName() + " " + recorderSuffix + ";";

            // add a field with a static array containing list of profiles
            instrString += generateCloverProfilesField(profiles);

            // add a lambdaInc() wrapper method for lambdas - only for java8 or higher
            if (isJava8OrHigher) {
                instrString += generateLambdaIncMethod(recorderSuffix);
            }

            // static initialization block
            instrString += "static{";

            //CoverageRecorder _REC = null;
            instrString += CoverageRecorder.class.getName() + " _" + recorderSuffix + "=null;";

            if (reportInitErrors) {
                instrString += "try{"
                        + (shouldEmitWarningMethod ? ($CloverVersionInfo$oldVersionInClasspath() + ";") : "")
                        + "if(" + CloverVersionInfo.getBuildStamp() + "L!="
                        + $CloverVersionInfo$getBuildStamp() + ")" + "{" + $Clover$l("\"" + INCOMPATIBLE_MSG + "\"") + ";"
                        + $Clover$l("\"[CLOVER] WARNING: Instr=" + CloverVersionInfo.getReleaseNum()
                        + "#" + CloverVersionInfo.getBuildStamp() + ",Runtime=\"+"
                        + $CloverVersionInfo$getReleaseNum() + "+\"#\"+" + $CloverVersionInfo$getBuildStamp()) + ";}";
            }

            //REC = Clover.getRecorder(....);
            //We make this initial assignment first so that the class the instrumenting class is shadowing
            //is re-entrant w.r.t. instrumentation.
            //
            //ie once TheirClass.__CLR.<clinit> has passed this point
            //we will not NPE if somehow there is a call on TheirClass.__CLR.R.inc(int) etc
            //The cost of this guard is that __CLR.R is no longer final.
            instrString += recorderSuffix + "=" + $Clover$getNullRecorder() + ";";

            //Assign local to the Clover.getNullRecorder() first, in case we fail to get the real recorder.
            //A null reference for a recorder will cause NPEs in the instrumented code
            //_REC = Clover.getNullRecorder();
            instrString += "_" + recorderSuffix + "=" + $Clover$getNullRecorder() + ";";

            //_REC = Clover.getRecorder(....);
            instrString += "_" + recorderSuffix + "="
                    + $Clover$getRecorder(
                    asUnicodeString(initString),
                    registryVersion + "L",
                    recorderCfg + "L",
                    Integer.toString(maxDataIndex),
                    "profiles",
                    "new " + javaLangPrefix + "String[]{\"" + CloverNames.PROP_DISTRIBUTED_CONFIG + "\"," + asUnicodeString(distributedConfig) + "}") + ";";

            if (reportInitErrors) {
                instrString += "}catch(" + javaLangPrefix + "SecurityException e){" + javaLangPrefix + "System.err.println(\"" + Clover.SECURITY_EXCEPTION_MSG
                        + " (\"+e.getClass()+\":\"+e.getMessage()+\")\");";
                instrString += "}catch(" + javaLangPrefix + "NoClassDefFoundError e){" + javaLangPrefix + "System.err.println(\"" + classNotFoundMsg
                        + " (\"+e.getClass()+\":\"+e.getMessage()+\")\");";
                instrString += "}catch(" + javaLangPrefix + "Throwable t){" + javaLangPrefix + "System.err.println(\"" + UNEXPECTED_MSG
                        + " (\"+t.getClass()+\":\"+t.getMessage()+\")\");}";
            }
            //REC = _REC
            instrString += recorderSuffix + "=" + "_" + recorderSuffix + ";";

            instrString += "}}";

            // add extra test sniffer field (required by test classes), note that it's not inside static inner recorder
            // class and that it has less 'random' name (independent of a file/class index)
            // the sniffer field is always generated, also for non-test classes, because we may have a non-test top-level
            // class containing inner or inline test classes (and the inner/inline classes don't have their own
            // recorder instance - they reuse a recorder instance from the top-level class)
            instrString += generateTestSnifferField(isSpockTestClass, isParameterizedJUnitTestClass, stateView.isJUnit5ParameterizedTest());

        } else {
            instrString = "public static "
                    + CoverageRecorder.class.getName() + " "
                    + recorderPrefix + "="
                    + $Clover$getRecorder(
                    asUnicodeString(initString),
                    registryVersion + "L",
                    recorderCfg + "L",
                    Integer.toString(maxDataIndex),
                    generateCloverProfilesInline(profiles),
                    "new " + javaLangPrefix + "String[]{\"" + CloverNames.PROP_DISTRIBUTED_CONFIG + "\"," + asUnicodeString(distributedConfig) + "}") + ";";
            // the sniffer field is always generated, also for non-test classes, see comment above
            instrString += generateTestSnifferField(isSpockTestClass, isParameterizedJUnitTestClass, stateView.isJUnit5ParameterizedTest());
        }
        return instrString;
    }

    static String generateTestSnifferField(boolean isSpock, boolean isParamJUnit, boolean isJunit5ParamTest) {
        return generateTestSnifferField(
                isSpock ? SnifferType.SPOCK :
                        (isParamJUnit || isJunit5ParamTest ? SnifferType.JUNIT : SnifferType.NULL));
    }

    /**
     * Generate declaration of the field named {@link CloverNames#CLOVER_TEST_NAME_SNIFFER}
     *
     * @param snifferType null, junit or spock
     * @return String text with a field declaration
     */
    /*private*/
    static String generateTestSnifferField(final SnifferType snifferType) {
        // public static final TestNameSniffer __CLRx_y_z_TEST_NAME_SNIFFER
        final String snifferField = "public static final " + TestNameSniffer.class.getName()
                + " " + CloverNames.CLOVER_TEST_NAME_SNIFFER;
        switch (snifferType) {
            case JUNIT:
            case SPOCK:
                // ... = new TestNameSniffer.Simple();
                return snifferField + "=new com_atlassian_clover.TestNameSniffer.Simple();";
            case NULL:
            default:
                // ... = TestNameSniffer.NULL_INSTANCE;
                return snifferField + "=" + TestNameSniffer.class.getName() + ".NULL_INSTANCE;";
        }
    }

    /**
     * Returns a string containing declaration of a generic method for wrapping lambda expressions:
     *
     * <pre>
     *    index - index of the hits counts array to be incremented (method entry)
     *    lambda - lambda function to be wrapped
     *    stmtIndex - index of the hits counts array to be incremented (statement entry)
     *    <I> - interface which lambda class implements (derived by compiler via type inference)
     *    <T> - lambda class having function code to be executed
     *    returns I - interface implemented by lambda
     *
     * @ java.lang.SuppressWarnings("unchecked")
     * public static <I, T extends I> I lambdaInc(final int index, final T lambda, final int stmtIndex) {
     *   java.lang.reflect.InvocationHandler handler = new java.lang.reflect.InvocationHandler() {
     *       public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
     *           inc(index);
     *           inc(stmtIndex);
     *           try {
     *               return method.invoke(lambda, args);
     *           } catch (InvocationTargetException ex) {
     *               throw ex.getCause() != null ? ex.getCause() : new RuntimeException("Clover failed to invoke instrumented lambda", ex);
     *           }
     *       }
     *   };
     *   return (I)Proxy.newProxyInstance(lambda.getClass().getClassLoader(),
     *           lambda.getClass().getInterfaces(), handler);
     * }
     * </pre>
     *
     * A reason why this method is being declared in every instrumented class and not in the <code>CoverageRecorder</code>
     * is that the proxy instance is unable to access non-public interfaces - it ends with an error
     * [java.lang.IllegalAccessException: Class CoverageRecorder$1 can not access a member of
     * class Xyz with modifiers "public abstract"].
     *
     * Therefore in order to wrap a lambda implementing non-public interface, our wrapper must be in the same scope.
     *
     * @return String code for "lambdaInc"
     */
    private String generateLambdaIncMethod(final String recorderSuffix) {
        // using variable names as short as possible to compress the code
        final StringBuilder str = new StringBuilder()
                .append("@java.lang.SuppressWarnings(\"unchecked\") ")
                .append("public static <I, T extends I> I ")
                .append(LAMBDA_INC_METHOD)
                .append("(final int i,final T l,final int si){")
                .append("java.lang.reflect.InvocationHandler h=new java.lang.reflect.InvocationHandler(){")
                .append("public ")
                .append(javaLangPrefix)
                .append("Object invoke(")
                .append(javaLangPrefix)
                .append("Object p,java.lang.reflect.Method m,")
                .append(javaLangPrefix)
                .append("Object[] a) ")
                .append("throws Throwable{")
                .append(recorderSuffix)
                .append(".inc(i);")
                .append(recorderSuffix)
                .append(".inc(si);")
                .append("try{return m.invoke(l,a);}catch(java.lang.reflect.InvocationTargetException e){")
                .append("throw e.getCause()!=null?e.getCause():new RuntimeException(\"Clover failed to invoke instrumented lambda\",e);")
                .append("}}};")
                .append("return (I)java.lang.reflect.Proxy.newProxyInstance(l.getClass().getClassLoader(),l.getClass().getInterfaces(),h);")
                .append("}");
        return str.toString();
    }

    /**
     * Return a string containing declaration of a field with a static array containing
     * list of profiles. Example:
     *
     * <pre>
     * public static CloverProfile[] profiles = {
     *    new CloverProfile("default", "FIXED", "host=localhost;timeout=500"),
     *    new CloverProfile("shared", "SHARED", null),
     *    ...
     * };
     * </pre>
     *
     * @return String
     */
    static String generateCloverProfilesField(List<CloverProfile> profiles) {
        // public static CloverProfile[] profiles = {
        String str = "public static " + CloverProfile.class.getName() + "[] profiles = { ";
        str += generateCloverProfilesNewInstances(profiles);
        str += "};";
        return str;
    }

    /**
     * Return a string containing inline creation of the CloverProfile array. Example:
     *
     * <pre>
     * new CloverProfile[] {
     *    new CloverProfile("default", "FIXED", "host=localhost;timeout=500"),
     *    new CloverProfile("shared", "SHARED", null),
     *    ...
     * }
     * </pre>
     *
     * @return String
     */
    public static String generateCloverProfilesInline(List<CloverProfile> profiles) {
        // new CloverProfile[] { ... }
        return "new " + CloverProfile.class.getName() + "[] {"
                + generateCloverProfilesNewInstances(profiles)
                + "}";
    }

    /**
     * Helper method for {@link #generateCloverProfilesField(java.util.List)}
     *
     * @param profiles list of runtime profiles
     * @return String
     */
    private static String generateCloverProfilesNewInstances(List<CloverProfile> profiles) {
        StringBuilder str = new StringBuilder();
        if (profiles != null) {
            for (Iterator<CloverProfile> iter = profiles.iterator(); iter.hasNext(); ) {
                CloverProfile profile = iter.next();
                // new CloverProfile(
                str.append("new ").append(CloverProfile.class.getName()).append("(");
                // "default",
                str.append(asUnicodeString(profile.getName())).append(", ");
                // "FIXED",
                str.append("\"").append(profile.getCoverageRecorder()).append("\", ");
                // "host=localhost;timeout=500") or null)
                if (profile.getDistributedCoverage() != null) {
                    str.append(asUnicodeString(profile.getDistributedCoverage().getConfigString())).append(")");
                } else {
                    str.append("null)");
                }
                if (iter.hasNext()) {
                    str.append(",");
                }
            }
        }
        return str.toString();
    }

    /**
     * Returns a unicode representation of the provided string, for example:
     * "abc" -> "\u0061\u0062\u0063"
     * In addition it doubles every "\" character in output sequence.
     */
    public static String asUnicodeString(String str) {
        if (str == null) {
            return "null";
        }

        StringBuilder res = new StringBuilder("\"");
        for (char c : str.toCharArray()) {
            // Because of fact that unicode escape sequences are being processed by javac compiler at the very beginning
            // (they are just being treated as an alternative way of writing characters in a text file) and a fact that
            // we will put unicode string into a java source file we must double the backslash character, because first
            // backslash will be treated as an escape character and not a backslash.
            //
            // Example without double backslash:
            //  '\tea' --to-unicode-> "\u005c\u0074\u0065\u0061"       --javac-> "\tea"  = '<tab>ea'
            // Example with double backslash:
            //  '\tea' --to-unicode-> "\u005c\u005c\u0074\u0065\u0061" --javac-> "\\tea" = '\tea'
            if (c == '\\') {
                res.append(String.format(Locale.US, "\\u%04x\\u%04x", (int) c, (int) c));
            } else {
                res.append(String.format(Locale.US, "\\u%04x", (int) c));
            }
        }
        res.append("\"");
        return res.toString();
    }

    public void setMaxDataIndex(int maxIndex) {
        maxDataIndex = maxIndex;
    }

    private static long getConfigBits(InstrumentationConfig cfg) {
        return CoverageRecorder.getConfigBits(cfg.getFlushPolicy(), cfg.getFlushInterval(), false, false, !cfg.isSliceRecording());
    }
}
