package org.openclover.buildutil.test.junit

import groovy.transform.CompileStatic
import junit.framework.TestCase

/** Base classes for tests with dynamically generated names  */
@CompileStatic
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