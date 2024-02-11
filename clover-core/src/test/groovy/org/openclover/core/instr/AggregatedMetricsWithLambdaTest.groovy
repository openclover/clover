package org.openclover.core.instr

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.openclover.util.Maps.newHashMap

/**
 * Test for {@link com.atlassian.clover.instr.InstrumentationSessionImpl} checking how aggregated metrics
 * are calculated for classes and methods with Java8 lambdas.
 */
class AggregatedMetricsWithLambdaTest extends AggregatedMetricsTestBase {

    @Rule
    public TestName name = new TestName()

    @Override
    protected String getTestName() {
        return name.getMethodName()
    }

    @Override
    protected String getTestFileBaseName() {
        return "AggregatedMetricsWithLambdaTest"
    }

    /**
     * Test if aggregatedStatements and aggregatedComplexity for classes are calculated properly when a lambda
     * expression is defined and assigned to a field or declared inside a method.
     */
    @Test
    void testAggregatedClassMetricsWithLambda() {
        // expected results
        final Map<RegistryKey, MetricValue> expClassMetrics = newHashMap()
        expClassMetrics.put(new RegistryKey("AggregatedMetrics"), new MetricValue(5, 9, 5, 8))
        expClassMetrics.put(new RegistryKey("AggregatedMetrics.B"), new MetricValue(4, 4, 3, 3))

        // check class metrics
        for (Map.Entry<RegistryKey, MetricValue> entry : expClassMetrics.entrySet()) {
            final String message = "failed for " + entry.getKey().className
            assertClassMetrics(message, entry.getValue(), findClass(packageInfo, entry.getKey()))
        }
    }

    /**
     * Test if aggregatedStatements and aggregatedComplexity for classes are calculated properly when a lambda
     * expression is defined and assigned to a field or declared inside a method.
     */
    @Test
    void testAggregatedMethodMetricsWithLambda() {
        // Map( (class name, method name) -> (statements, aggregated statements) )
        final Map<RegistryKey, MetricValue> expMethodMetrics = newHashMap()
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics", '$lam#0'), new MetricValue(1, 1, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics", 'methodTwo'), new MetricValue(4, 4, 3, 3))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics", '$lam#3'), new MetricValue(2, 2, 2, 2)); //lam#3 encloses lam#4
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics", '$lam#4'), new MetricValue(1, 1, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics", 'methodThree'), new MetricValue(0, 0, 1, 1))

        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.B", '$lam#1'), new MetricValue(1, 1, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.B", '$lam#2'), new MetricValue(1, 1, 1, 1))
        expMethodMetrics.put(new RegistryKey("AggregatedMetrics.B", 'methodOne'), new MetricValue(3, 3, 2, 2))

        // check method metrics
        for (Map.Entry<RegistryKey, MetricValue> entry : expMethodMetrics.entrySet()) {
            final String message = "failed for " + entry.getKey().className + "." + entry.getKey().methodName
            assertMethodMetrics(message, entry.getValue(), findMethod(packageInfo, entry.getKey()))
        }
    }

}
