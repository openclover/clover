package com.atlassian.clover.test

/**
 * Tests how Clover integrates with JUnit4 in order to intercept test names at runtime.
 * Test is using 'test/resources/parameterized-junit4' code sample.
 */
class ParameterizedJUnit4Test extends ParameterizedJUnitTestBase {

    ParameterizedJUnit4Test(String name) {
        super(name)
    }

    @Override
    String getCodeExampleDir() {
        return "${TEST_RESOURCES_DIR}${File.separator}parameterized-junit4"
    }

}
