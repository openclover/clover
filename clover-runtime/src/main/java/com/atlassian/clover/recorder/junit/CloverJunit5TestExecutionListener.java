package com.atlassian.clover.recorder.junit;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import com_atlassian_clover.JUnitParameterizedTestSniffer;
import com_atlassian_clover.Junit5ParameterizedTestSniffer;
import com_atlassian_clover.TestNameSniffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.lang.reflect.Field;

/**
 * <p>Clover's Test Execution Listener to be used when tests are run using Jnit5 (i.e. Junit Platform).
 * Use {@link JUnitTestRunnerInterceptor} if tests are run using Junit 4.</p>
 *
 * <p>This Listener supports both Junit4 Parameterized tests and Junit 5 Parameterized tests.</p>
 *
 * <p>To add this listener to the project, just add this class in {@code }/META-INF/services/org.junit.platform.launcher.TestExecutionListener}</p>
 */
public class CloverJunit5TestExecutionListener implements TestExecutionListener {

    private TestPlan testPlan;
    private TestNameSniffer junitSniffer;


    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    public void executionStarted(TestIdentifier testIdentifier) {
        Logger.getInstance().warn("CloverJunit5TestExecutionListener: isTest()? "+testIdentifier.isTest());

        if (testIdentifier.isTest()) {
            /* Unfortunately, junit 5 doesn't have a reliable way to find the current test running class.
             * Hence, using a work around suggested  at the following link.
             * https://github.com/junit-team/junit5/issues/737
             */
            Logger.getInstance().debug("CloverJunit5TestExecutionListener: JUnitPlatform test started: \""
                    + testIdentifier.getDisplayName() + "\"");

            // find Clover's field in a test class and pass test information
            //final Class testClass = testIdentifier.getTestClass();
            Class testClass = findTestMethodClassName(this.testPlan, testIdentifier);
            Logger.getInstance().warn("CloverJunit5TestExecutionListener: testClass? "+testClass);

            if (testClass != null) {
                Logger.getInstance().warn("CloverJunit5TestExecutionListener: junitSniffer? "+junitSniffer);
                junitSniffer = lookupTestSnifferField(testClass);
                if (junitSniffer != null) {
                    if (junitSniffer instanceof Junit5ParameterizedTestSniffer) {
                        /* TODO: Junit 5 Parameterized Test's Display name is not returing method name.
                         *  Is the method name needed in the test name? If not, below StringBuilder is not needed.
                         */
                        Logger.getInstance().warn("CloverJunit5TestExecutionListener: junit 5 Sniffer");
                        String testName = getMethodName(testIdentifier) +
                                "[" + testIdentifier.getDisplayName() + "]";
                        ((Junit5ParameterizedTestSniffer) junitSniffer).testStarted(testName);
                    } else if (junitSniffer instanceof JUnitParameterizedTestSniffer) {
                        /* TODO: Junit 4 Parameterized Test's Display name is not returing Class name. Appending class name
                         *  to be consistant with JUnitTestRunnerInterceptor. Is the class name needed in the test name?
                         *  If not, below StringBuilder is not needed.
                         */
                        Logger.getInstance().warn("CloverJunit5TestExecutionListener: junit 4 Sniffer");
                        String testName = testIdentifier.getDisplayName() + "(" +
                                testClass.getName() + ")";
                        ((JUnitParameterizedTestSniffer) junitSniffer).testStarted(testName);
                    }
                }
            }
        }
    }

    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (junitSniffer instanceof Junit5ParameterizedTestSniffer) {
            ((Junit5ParameterizedTestSniffer) junitSniffer).testEnded();
        } else if (junitSniffer instanceof JUnitParameterizedTestSniffer) {
            ((JUnitParameterizedTestSniffer) junitSniffer).testEnded("");
        }
    }

    /**
     * Get Test Method Name from TestIdentifier.
     *
     * @param identifier TestIdentifier for which the method name will be queried.
     * @return - test method name, or empty string if a MethodSource is not found in the given input parameter.
     */
    @NotNull
    private static String getMethodName(TestIdentifier identifier) {
        String methodName = "";
        if (!(identifier.getSource().isPresent())) {
            throw new IllegalStateException("identifier must contain MethodSource");
        }
        TestSource source = identifier.getSource().get();
        if (!(source instanceof MethodSource)) {
            throw new IllegalStateException("identifier must contain MethodSource");
        }
        if (source instanceof MethodSource) {
            methodName = ((MethodSource) source).getMethodName();
        }
        return methodName;

    }

    private static Class findTestMethodClassName(TestPlan testPlan, TestIdentifier identifier) {
        if (!(identifier.getSource().isPresent())) {
            throw new IllegalStateException("identifier must contain MethodSource");
        }
        TestSource source = identifier.getSource().get();
        if (!(source instanceof MethodSource)) {
            throw new IllegalStateException("identifier must contain MethodSource");
        }
        TestIdentifier current = identifier;
        while (current != null) {
            if (current.getSource().isPresent() && current.getSource().get() instanceof ClassSource) {

                return ((ClassSource) current.getSource().get()).getJavaClass();
            }
            current = testPlan.getParent(current).orElse(null);
        }
        throw new IllegalStateException("Class name not found");
    }

    /**
     * Find the CloverNames.CLOVER_TEST_NAME_SNIFFER field in the current instance of a test class Return instance
     * assigned to this field if it's a JUnitParameterizedTestSniffer or <code>null</code> otherwise.
     *
     * @return JUnitParameterizedTestSniffer instance or <code>null</code>
     */
    @Nullable
    protected TestNameSniffer lookupTestSnifferField(Class currentTestClass) {
        try {
            Field sniffer = currentTestClass.getField(CloverNames.CLOVER_TEST_NAME_SNIFFER);
            if (sniffer.getType().isAssignableFrom(TestNameSniffer.class)) {
                Object snifferObj = sniffer.get(null);
                if(snifferObj instanceof TestNameSniffer){
                    return (TestNameSniffer) snifferObj;
                };
            } else {
                Logger.getInstance().debug("Unexpected type of the "
                        + CloverNames.CLOVER_TEST_NAME_SNIFFER + " field: " + sniffer.getType().getName()
                        + " - ignoring. Test name found during instrumentation may differ from the actual name of the test at runtime.");
            }
        } catch (NoSuchFieldException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                            + " was not found in an instance of " + currentTestClass.getClass().getName()
                            + ". Test name found during instrumentation may differ from the actual name of the test at runtime.",
                    ex);
        } catch (SecurityException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                            + " couldn't be accessed in an instance of " + currentTestClass.getClass().getName()
                            + ". Test name found during instrumentation may differ from the actual name of the test at runtime."
                    , ex);
        } catch (IllegalAccessException ex) {
            Logger.getInstance().debug("Field " + CloverNames.CLOVER_TEST_NAME_SNIFFER
                            + " couldn't be accessed in an instance of " + currentTestClass.getClass().getName()
                            + ". Test name found during instrumentation may differ from the actual name of the test at runtime."
                    , ex);
        }

        // error when searching / accesing the field; return null
        return null;
    }

}
