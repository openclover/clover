package com.atlassian.clover.instr

import clover.com.google.common.collect.Maps
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.assertEquals

/**
 * Test for {@link InstrumentationSessionImpl} checking how aggregated metrics are calculated for top-level, inner and
 * anonymous inline classes.
 */
class AggregatedMetricsWithInnerAndInlineClassesTest extends AggregatedMetricsTestBase {

    @Rule
    public TestName name = new TestName()

    @Override
    protected String getTestName() {
        return name.getMethodName()
    }

    @Override
    protected String getTestFileBaseName() {
        return "AggregatedMetricsWithInnerAndInlineClassesTest"
    }

    /**
     * Set up temporary directory
     */
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    /**
     * Test if aggregatedStatements and aggregatedComplexity for classes are calculated properly.
     */
    @Test
    void testAggregatedMetricsForClasses() {
        // expected results
        final Map<RegistryKey, MetricValue> expClassMetrics = Maps.newHashMap()
        expClassMetrics.put(new RegistryKey("AggregatedMetrics"), new MetricValue(1, 15, 1, 15))
        expClassMetrics.put(new RegistryKey("AggregatedMetrics.B"), new MetricValue(3, 3, 2, 2))
        expClassMetrics.put(new RegistryKey("AggregatedMetrics.C"), new MetricValue(5, 5, 5, 5))
        expClassMetrics.put(new RegistryKey("AggregatedMetrics.E"), new MetricValue(6, 6, 7, 7))
        expClassMetrics.put(new RegistryKey("AggregatedComplexityTest"), new MetricValue(5, 5, 7, 7))
        // Note that Clover does not have ClassInfo for anonymous classes
        // expClassMetrics.put(new RegistryKey("AggregatedMetrics.E$1"), new MetricValue(2, 4, -1))
        // expClassMetrics.put(new RegistryKey("AggregatedMetrics.E$1$1"), new MetricValue(2, 2, -1))

        // check class metrics
        for (Map.Entry<RegistryKey, MetricValue> entry : expClassMetrics.entrySet()) {
            final String message = "failed for " + entry.getKey().className
            assertClassMetrics(message, entry.getValue(), findClass(packageInfo, entry.getKey()))
        }
    }

    /**
     * Test if aggregatedStatements and aggregatedComplexity for classes are calculated properly.
     */
    @Test
    void testAggregatedMetricsForMethods() {
        // Map( (class name, method name) -> (statements, aggregated statements) )
        final Map<RegistryKey, MetricValue> expMethodMetrics = Maps.newHashMap()
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics", "myMethod"), new MetricValue(1, 1, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.C", "methodThree"), new MetricValue(2, 4, 1, 4))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.C", "methodFour"), new MetricValue(1, 1, 1, 1))
        // Note that methods from anonymous classes are added to parent class
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.C", "hasNext"), new MetricValue(1, 1, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.C", "next"), new MetricValue(1, 1, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.C", "remove"), new MetricValue(0, 0, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.E", "methodFive"), new MetricValue(2, 6, 1, 7))
        expMethodMetrics.put(new RegistryKey("AggregatedComplexityTest", "hasNext"), new MetricValue(4, 4, 4, 4))
        expMethodMetrics.put(new RegistryKey("AggregatedComplexityTest", "next"), new MetricValue(1, 1, 2, 2))

        // check method metrics
        for (Map.Entry<RegistryKey, MetricValue> entry : expMethodMetrics.entrySet()) {
            final String message = "failed for " + entry.getKey().className + "." + entry.getKey().methodName
            assertMethodMetrics(message, entry.getValue(), findMethod(packageInfo, entry.getKey()))
        }
    }

    @Test
    void testDoubledMethodsForInlineClasses() {
        // Class E contains doubled methods due to double nesting of anonymous Iterator class, validate it
        assertEquals(2, findAllMethods(packageInfo, new RegistryKey("AggregatedMetrics.E", "next")).size())
        assertEquals(2, findAllMethods(packageInfo, new RegistryKey("AggregatedMetrics.E", "hasNext")).size())
        assertEquals(2, findAllMethods(packageInfo, new RegistryKey("AggregatedMetrics.E", "remove")).size())
    }

}
