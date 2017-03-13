package com_atlassian_clover;

import junit.runner.TestRunListener;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * Test name sniffer which handles JUnit 3.x and 4.x Runner callbacks
 */
public class JUnitParameterizedTestSniffer extends RunListener implements TestRunListener, TestNameSniffer  {

    /**
     * Value holder
     */
    BaseTestNameSniffer testNameSniffer = new BaseTestNameSniffer() {
        // using default implementation
    };

    // JUnit 3

    @Override
    public void testRunStarted(String s, int i) {
        // ignored
    }

    @Override
    public void testRunEnded(long l) {
        // ignored
    }

    @Override
    public void testRunStopped(long l) {
        // ignored
    }

    @Override
    public void testStarted(String testName) {
        testNameSniffer.setTestName(testName);
    }

    @Override
    public void testEnded(String s) {
        testNameSniffer.clearTestName();
    }

    @Override
    public void testFailed(int i, String s, String s2) {

    }

    // JUnit 4

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) throws Exception {
        testNameSniffer.setTestName(description.getDisplayName());
    }

    @Override
    public void testFinished(Description description) throws Exception {
        testNameSniffer.clearTestName();
    }

    @Override
    public String getTestName() {
        return testNameSniffer.getTestName();
    }
}
