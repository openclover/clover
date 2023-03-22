package com.atlassian.clover.test.junit

import junit.framework.TestCase

/** Base classes for tests with dynamically generated names  */
class DynamicallyNamedTestBase extends TestCase {
    String methodName

    DynamicallyNamedTestBase(String testName) {
        super(testName)
        this.methodName = testName
    }

    DynamicallyNamedTestBase(String methodName, String specificName) {
        super(specificName)
        this.methodName = methodName
    }

    protected void runTest() {
        def compoundName = getName()
        setName(methodName)
        try {
            super.runTest()
        } finally {
            setName(compoundName)
        }
    }
}