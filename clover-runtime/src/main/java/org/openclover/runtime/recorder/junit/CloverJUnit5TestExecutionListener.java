package org.openclover.runtime.recorder.junit;

import org.openclover.runtime.Logger;
import org.openclover.runtime.recorder.TestNameSnifferHelper;
import com_atlassian_clover.TestNameSniffer;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Optional;

/**
 * <p>Clover's Test Execution Listener to be used when tests are run using JUnit5 (i.e. JUnit Platform).
 * Use {@link JUnitTestRunnerInterceptor} if tests are run using Junit 4.</p>
 *
 * <p>This Listener supports both Junit4 Parameterized tests and Junit 5 Parameterized tests.</p>
 *
 * <p>To add this listener to the project, just add this class in {@code }/META-INF/services/org.junit.platform.launcher.TestExecutionListener}</p>
 */
public class CloverJUnit5TestExecutionListener implements TestExecutionListener {

    private TestPlan testPlan;


    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    public void executionStarted(TestIdentifier testIdentifier) {
        Logger.getInstance().debug("CloverJUnit5TestExecutionListener: JUnit test started: \"" + testIdentifier.getDisplayName() + "\"");

        if (testIdentifier.isTest()) {
            final String testName = testIdentifier.getDisplayName(); // always non-null and non-empty as per API

            // find Clover's field in a test class and pass test information
            final Class testClass = findTestMethodClass(this.testPlan, testIdentifier);

            if (testClass != null) {
                final TestNameSniffer junitSniffer = TestNameSnifferHelper.lookupTestSnifferField(testClass);

                if (junitSniffer != null) {
                    junitSniffer.setTestName(testName);
                }
            }
        }
    }

    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        Logger.getInstance().debug("CloverJUnit5TestExecutionListener: JUnit test ended: \"" + testIdentifier.getDisplayName() + "\"");

        if (testIdentifier.isTest()) {
            final Class testClass = findTestMethodClass(this.testPlan, testIdentifier);

            if (testClass != null) {
                final TestNameSniffer junitSniffer = TestNameSnifferHelper.lookupTestSnifferField(testClass);

                if (junitSniffer != null) {
                    junitSniffer.clearTestName();
                }
            }
        }
    }

    /**
     * Unfortunately, JUnit 5 doesn't have a reliable way to find the current test running class. Hence, using a
     * work-around suggested  at the following link. <a href="https://github.com/junit-team/junit5/issues/737">issue 737</a>
     */
    /*@Nullable*/
    private static Class findTestMethodClass(TestPlan testPlan, TestIdentifier identifier) {
        // method source
        Class javaClass = fromMethodSource(identifier);
        if (javaClass != null) {
            return javaClass;
        }

        // class source
        javaClass = fromClassSource(identifier);
        if (javaClass != null) {
            return javaClass;
        }

        // class source, but we have to look it up in the test hierarchy
        for (TestIdentifier iter = identifier;
             testPlan.getParent(iter).isPresent();
             iter = testPlan.getParent(iter).get()) {

            javaClass = fromClassSource(iter);
            if (javaClass != null) {
                return javaClass;
            }
        }

        return null;
    }

    /*@Nullable*/
    private static Class fromMethodSource(final TestIdentifier identifier) {
        final Optional<TestSource> source = identifier.getSource();
        if (source.isPresent() && source.get() instanceof MethodSource) {
            try {
                return Class.forName(((MethodSource) source.get()).getClassName());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    /*@Nullable*/
    private static Class fromClassSource(final TestIdentifier identifier) {
        final Optional<TestSource> source = identifier.getSource();
        if (source.isPresent() && source.get() instanceof ClassSource) {
            try {
                return ((ClassSource) source.get()).getJavaClass();
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

}
