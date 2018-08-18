package com.atlassian.clover.instr.tests;

public final class TestAnnotationNames {
    // common
    public static final String TEST_ANNO_NAME = "Test";
    public static final String VALUE_ATTR_NAME = "value";

    // TestNG
    public static final String TESTNG_FQ_TEST_ANNO_NAME = "org.testng.annotations.Test";
    public static final String TESTNG_FQ_EXPECTED_ANNO_NAME = "org.testng.annotations.ExpectedExceptions";
    public static final String TESTNG_EXPECTED_ANNO_NAME = "ExpectedExceptions";
    public static final String EXPECTED_EXCEPTIONS_ATTR_NAME = "expectedExceptions";

    // Spring
    public static final String SPRING_FQ_EXPECTED_ANNO_NAME = "org.springframework.test.annotation.ExpectedException";
    public static final String SPRING_EXPECTED_ANNO_NAME = "ExpectedException";

    // JUnit4
    public static final String ORG_JUNIT_NAME = "org.junit";
    public static final String JUNIT_TEST_ANNO_NAME = ORG_JUNIT_NAME + ".Test";
    public static final String JUNIT_IGNORE_ANNO_NAME = ORG_JUNIT_NAME + ".Ignore";
    public static final String IGNORE_ANNO_NAME = "Ignore";
    public static final String EXPECTED_ATTR_NAME = "expected";

    // JUnit5
    public static final String ORG_JUNIT5_NAME = "org.junit.jupiter.api";
    public static final String JUNIT5_TEST_ANNO_NAME = ORG_JUNIT5_NAME + ".Test";
    public static final String JUNIT5_IGNORE_ANNO_NAME = ORG_JUNIT5_NAME + ".Disabled";
    public static final String DISABLED_ANNO_NAME = "Disabled";

    // Instinct framework
    public static final String SPECIFICATION_ANNO_NAME = "Specification";
    public static final String INSTINCT_SPECIFICATION_ANNO_NAME = "com.googlecode.instinct.marker.annotate." + SPECIFICATION_ANNO_NAME;
    public static final String EXPECTED_EXCEPTION_ATTR_NAME = "expectedException";

    // Spock framework
    public static final String SPOCK_METHOD_FQ_ANNO_NAME = "org.spockframework.runtime.model.FeatureMetadata"; // for a method
    public static final String SPOCK_METHOD_ANNO_NAME = "FeatureMetadata";
    public static final String SPOCK_CLASS_FQ_ANNO_NAME = "org.spockframework.runtime.model.SpecMetadata"; // for a class
    public static final String SPOCK_CLASS_ANNO_NAME = "SpecMetadata";
}
