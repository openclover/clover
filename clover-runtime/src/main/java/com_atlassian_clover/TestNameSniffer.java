package com_atlassian_clover;

import org.jetbrains.annotations.Nullable;

/**
 * A interface for a class which can "listen" a test runner and return name of the currently executing test.
 * This can be useful for parameterized tests, which have a test name different than the name of the underlying
 * test method, for instance for Spock features or for JUnit4 parameterized tests.
 */
public interface TestNameSniffer {

    /**
     * Implementation returning null as the test name
     */
    class Null implements TestNameSniffer {
        @Override
        @Nullable
        public String getTestName() {
            return null;
        }

        @Override
        public void setTestName(@Nullable String testName) {
            // no op
        }

        @Override
        public void clearTestName() {
            // no op
        }
    }

    /**
     * Simple value holder.
     */
    class Simple implements TestNameSniffer {
        private transient String testName;

        @Override
        @Nullable
        public String getTestName() {
            return testName;
        }

        @Override
        public void setTestName(@Nullable String testName) {
            this.testName = testName;
        }

        @Override
        public void clearTestName() {
            testName = null;
        }
    }

    /**
     * A flyweight pattern. We reuse this instance by default in every class to avoid unnecessry object instantiation.
     */
    TestNameSniffer NULL_INSTANCE = new TestNameSniffer.Null();

    @Nullable
    String getTestName();

    void setTestName(@Nullable String testName);

    void clearTestName();
}
