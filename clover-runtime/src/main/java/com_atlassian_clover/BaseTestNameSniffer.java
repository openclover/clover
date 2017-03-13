package com_atlassian_clover;

import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of the test sniffer.
 */
public class BaseTestNameSniffer implements TestNameSniffer {
    private transient String testName;

    @Override
    @Nullable
    public String getTestName() {
        return testName;
    }

    public void setTestName(@Nullable String testName) {
        this.testName = testName;
    }

    public void clearTestName() {
        testName = null;
    }
}
