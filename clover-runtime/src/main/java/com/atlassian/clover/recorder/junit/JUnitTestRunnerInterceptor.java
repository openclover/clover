package com.atlassian.clover.recorder.junit;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import com_atlassian_clover.JUnitParameterizedTestSniffer;
import com_atlassian_clover.TestNameSniffer;
import junit.runner.TestRunListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import java.lang.reflect.Field;

/**
 *
 */
public class JUnitTestRunnerInterceptor extends RunListener implements TestRunListener {

    // JUnit4

    @Override
    public void testStarted(final Description description) throws Exception {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test started: \""
                + description.getDisplayName() + "\"");

        // find Clover's field in a test class and pass test information
        final Class testClass = description.getTestClass();
        if (testClass != null) {
            final JUnitParameterizedTestSniffer junitSniffer = lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.testStarted(description);
            }
        }
    }

    @Override
    public void testFinished(final Description description) throws Exception {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test ended: \""
                + description.getDisplayName() + "\"");

        // find Clover's field in a test class and pass test information
        final Class testClass = description.getTestClass();
        if (testClass != null) {
            final JUnitParameterizedTestSniffer junitSniffer = lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.testFinished(description);
            }
        }
    }

    // JUnit3

    @Override
    public void testStarted(String testName) {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test ended: \""
                + testName + "\"");

        // find Clover's field in a test class and pass test information
        final Class testClass = getTestClass(testName);
        if (testClass != null) {
            final JUnitParameterizedTestSniffer junitSniffer = lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.testStarted(testName);
            }
        }
    }

    @Override
    public void testEnded(String testName) {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test ended: \""
                + testName + "\"");

        // find Clover's field in a test class and pass test information
        final Class testClass = getTestClass(testName);
        if (testClass != null) {
            final JUnitParameterizedTestSniffer junitSniffer = lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.testEnded(testName);
            }
        }
    }

    // helper methods

    @Nullable
    protected Class getTestClass(@NotNull String testName) {
        // find test class from a test name
        int classNameEnd = testName.lastIndexOf(".");
        if (classNameEnd > 0) {   // at least one character for a class name
            String className = testName.substring(0, classNameEnd);
            try {
                // find an instance of the class
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                Logger.getInstance().debug("Unable to find class '" + className
                        + "'. Clover cannot inject the current test name into coverage recorder.");
            }
        } else {
            Logger.getInstance().debug("Unknown class name for a test. Clover cannot inject the current test name into coverage recorder");
        }

        return null;
    }

    /**
     * Find the CloverNames.CLOVER_TEST_NAME_SNIFFER field in the current instance of a test class Return instance
     * assigned to this field if it's a JUnitParameterizedTestSniffer or <code>null</code> otherwise.
     *
     * @return JUnitParameterizedTestSniffer instance or <code>null</code>
     */
    @Nullable
    protected JUnitParameterizedTestSniffer lookupTestSnifferField(Class currentTestClass) {
        try {
            Field sniffer = currentTestClass.getField(CloverNames.CLOVER_TEST_NAME_SNIFFER);
            if (sniffer.getType().isAssignableFrom(TestNameSniffer.class)) {
                Object snifferObj = sniffer.get(null);
                if (snifferObj instanceof JUnitParameterizedTestSniffer) {
                    return (JUnitParameterizedTestSniffer) snifferObj;
                } else {
                    // field which was found is not an instance of the spock runner sniffer (maybe a junit4 sniffer?)
                    return null;
                }
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

    // obsolete

    @Override
    public void testRunStarted(String testSuiteName, int testCount) {

    }

    @Override
    public void testRunEnded(long elapsedTime) {

    }

    @Override
    public void testRunStopped(long elapsedTime) {

    }

    @Override
    public void testFailed(int status, String testName, String trace) {

    }

}
