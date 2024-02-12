package org.openclover.runtime.recorder.junit;

import junit.runner.TestRunListener;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.openclover.runtime.Logger;
import org.openclover.runtime.recorder.TestNameSnifferHelper;
import org_openclover_runtime.TestNameSniffer;

/**
 *
 */
public class JUnitTestRunnerInterceptor extends RunListener implements TestRunListener {

    // JUnit4

    @Override
    public void testStarted(final Description description) {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test started: \""
                + description.getDisplayName() + "\"");

        // find Clover's field in a test class and pass test information
        final Class<?> testClass = description.getTestClass();
        if (testClass != null) {
            final TestNameSniffer junitSniffer = TestNameSnifferHelper.lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.setTestName(description.getDisplayName());
            }
        }
    }

    @Override
    public void testFinished(final Description description) {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test ended: \""
                + description.getDisplayName() + "\"");

        // find Clover's field in a test class and pass test information
        final Class<?> testClass = description.getTestClass();
        if (testClass != null) {
            final TestNameSniffer junitSniffer = TestNameSnifferHelper.lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.clearTestName();
            }
        }
    }

    // JUnit3

    @Override
    public void testStarted(String testName) {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test ended: \""
                + testName + "\"");

        // find Clover's field in a test class and pass test information
        final Class<?> testClass = getTestClass(testName);
        if (testClass != null) {
            final TestNameSniffer junitSniffer = TestNameSnifferHelper.lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.setTestName(testName);
            }
        }
    }

    @Override
    public void testEnded(String testName) {
        Logger.getInstance().debug("JUnitTestRunnerInterceptor: JUnit test ended: \""
                + testName + "\"");

        // find Clover's field in a test class and pass test information
        final Class<?> testClass = getTestClass(testName);
        if (testClass != null) {
            final TestNameSniffer junitSniffer = TestNameSnifferHelper.lookupTestSnifferField(testClass);
            if (junitSniffer != null) {
                junitSniffer.clearTestName();
            }
        }
    }

    // helper methods

    /*@Nullable*/
    protected Class<?> getTestClass(/*@NotNull*/ String testName) {
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
