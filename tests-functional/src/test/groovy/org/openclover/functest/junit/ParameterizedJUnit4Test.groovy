package org.openclover.functest.junit

import groovy.transform.CompileStatic

/**
 * Tests how Clover integrates with JUnit4 in order to intercept test names at runtime.
 * Test is using 'test/resources/parameterized-junit4' code sample.
 */
@CompileStatic
class ParameterizedJUnit4Test extends ParameterizedJUnitTestBase {

    ParameterizedJUnit4Test(String name) {
        super(name)
    }

    @Override
    String getCodeExampleDir() {
        return "${TEST_RESOURCES_DIR}${File.separator}parameterized-junit4"
    }

}
