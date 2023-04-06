package com.atlassian.clover.test

/**
 * Tests how Clover integrates with JUnit5 in order to intercept test names at runtime.
 * Test is using 'test/resources/parameterized-junit5' code sample.
 */
class ParameterizedJUnit5Test extends ParameterizedJUnitTestBase {

    ParameterizedJUnit5Test(String name) {
        super(name)
    }

    @Override
    String getCodeExampleDir() {
        return "${TEST_RESOURCES_DIR}${File.separator}parameterized-junit5"
    }

}
