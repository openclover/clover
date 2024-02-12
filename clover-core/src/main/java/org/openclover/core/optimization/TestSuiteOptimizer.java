package org.openclover.core.optimization;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.openclover.core.api.optimization.Optimizable;
import org.openclover.core.api.optimization.OptimizationOptions;
import org.openclover.core.api.optimization.TestOptimizer;

import java.util.Enumeration;
import java.util.List;

import static org.openclover.core.util.Lists.newLinkedList;

/**
 * An optimizer which optimizes a junit.framework.TestSuite of tests.
 * <p/>
 * This class currently won't optimize a TestSuite of TestSuites.
 * TODO: Support for suites of suites.
 * TODO: Move these methods to TestOptimizer
 */
public class TestSuiteOptimizer {

    private final OptimizationOptions options;

    /**
     * Creates an instance of TestSuiteOptimzer with the given options.
     * @param options the {@link OptimizationOptions} to use when optimizing.
     */
    public TestSuiteOptimizer(OptimizationOptions options) {
        this.options = options;
    }

    /**
     * Creates an instance TestSuiteOptimizer with default options.
     */
    public TestSuiteOptimizer() {
        this.options = new OptimizationOptions.Builder().build();
    }

    /**
     * Optimize the given suite of tests and return a new TestSuite instance with a reduced amount of tests.
     * The name of the returned TestSuite is the same as the name of suite parameter.
     * <p/>
     * This does not optimize a TestSuite of TestSuites.
     * 
     * @param suite the suite of tests to optimize
     * @return a new suite, containing an optimized set of tests.
     */
    public TestSuite optimize(TestSuite suite) {
        
        final TestSuite optimizedSuite = new TestSuite(suite.getName());

        final List<Optimizable> optimizables = createOptimizables(suite.tests());

        final TestOptimizer optimizer = new TestOptimizer(options);
        final List<Optimizable> optimized = optimizer.optimize(optimizables);

        // add the optimized tests to the optimizedSuite
        for (Optimizable optimizable: optimized) {
            final TestOptimizable test = (TestOptimizable)optimizable;
            optimizedSuite.addTest(test.getTest());
        }
        return optimizedSuite;
    }


    private List<Optimizable> createOptimizables(Enumeration tests) {
        final List<Optimizable> optimizableTests = newLinkedList();
        
        while (tests.hasMoreElements()) {
            final Test test = (Test) tests.nextElement();
            if (test instanceof TestSuite) { // a TestCase added via addTestSuite() is a TestSuite
                optimizableTests.add(new TestOptimizable((TestSuite) test));
            }
            // TODO handle TestSuites of TestCases? ie nested suites.
        }
        return optimizableTests;
    }
}
