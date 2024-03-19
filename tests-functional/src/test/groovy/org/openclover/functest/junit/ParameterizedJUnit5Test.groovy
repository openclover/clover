package org.openclover.functest.junit

import groovy.transform.CompileStatic

/**
 * Tests how OpenClover integrates with JUnit5 in order to intercept test names at runtime.
 * Test is using 'test/resources/parameterized-junit5' code sample.
 */
@CompileStatic
class ParameterizedJUnit5Test extends ParameterizedJUnitTestBase {

    ParameterizedJUnit5Test(String name) {
        super(name)
    }

    @Override
    String getCodeExampleDir() {
        return "${TEST_RESOURCES_DIR}${File.separator}parameterized-junit5"
    }

}
