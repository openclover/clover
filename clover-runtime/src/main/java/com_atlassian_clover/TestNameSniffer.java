package com_atlassian_clover;

import org.jetbrains.annotations.Nullable;

/**
 * A interface for a class which can "listen" a test runner and return name of the currently executing test.
 * This can be useful for parameterized tests, which have a test name different than the name of the underlying
 * test method, for instance for Spock features or for JUnit4 parameterized tests.
 */
public interface TestNameSniffer {
    /** Implementation returning null as the test name */
    TestNameSniffer NULL_INSTANCE = new TestNameSniffer() {
        @Override
        @Nullable
        public String getTestName() {
            return null;
        }
    };

    @Nullable
    String getTestName();
}
