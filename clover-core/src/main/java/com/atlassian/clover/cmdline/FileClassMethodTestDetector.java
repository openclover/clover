package com.atlassian.clover.cmdline;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.instr.tests.AggregateTestDetector;
import com.atlassian.clover.instr.tests.AndStrategy;
import com.atlassian.clover.instr.tests.AntPatternTestDetectorFilter;
import com.atlassian.clover.instr.tests.DefaultTestDetector;
import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.spec.instr.test.BooleanSpec;
import com.atlassian.clover.spec.instr.test.OrSpec;
import com.atlassian.clover.spec.instr.test.TestClassSpec;
import com.atlassian.clover.spec.instr.test.TestMethodSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileClassMethodTestDetector implements TestDetector {

    // we want to lazily get project root
    private final JavaInstrumentationConfig cfg;

    // figure out includes and excludes
    private TestDetector testDetector;

    private String includePattern;
    private String excludePattern;
    private List<TestClassSpec> testClassSpec = new ArrayList<TestClassSpec>();

    public FileClassMethodTestDetector(JavaInstrumentationConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return getTestDetector().isTypeMatch(sourceContext, typeContext);
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return getTestDetector().isMethodMatch(sourceContext, methodContext);
    }

    public void setIncludes(String includePattern) {
        this.includePattern = includePattern;
    }

    public void setExcludes(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    public void addTestClassSpec(TestClassSpec testClassSpec) {
        this.testClassSpec.add(testClassSpec);
    }

    /**
     * In case there's no TestClassSpec then create new one accepting any class.
     * Add TestMethodSpec to the last TestClassSpec.
     * @param testMethodSpec
     */
    public void addTestMethodSpec(TestMethodSpec testMethodSpec) {
        if (testClassSpec.isEmpty()) {
            final TestClassSpec anyClassWithMethod = new TestClassSpec();
            testClassSpec.add(anyClassWithMethod);
        }

        final TestClassSpec lastClassSpec = testClassSpec.get(testClassSpec.size() - 1);
        lastClassSpec.addConfiguredTestMethod(testMethodSpec);
    }

    private TestDetector getTestDetector() {
        if (testDetector == null) {
            testDetector = buildTestDetector();
        }
        return testDetector;
    }

    private TestDetector buildTestDetector() {

        try {
            final TestDetector includesExcludesTestDetector = new AntPatternTestDetectorFilter(
                    cfg.getSourceDir().getAbsolutePath(),
                    includePattern == null ? null : includePattern.split(","),
                    excludePattern == null ? null : excludePattern.split(","));

            // we must match all of: files, classes, methods
            final AggregateTestDetector aggregatedDetector = new AggregateTestDetector(new AndStrategy());
            aggregatedDetector.addDetector(includesExcludesTestDetector);

            // add a non-empty detector only, otherwise it will reject everything
            if (!testClassSpec.isEmpty()) {
                final BooleanSpec anyOfTheClasses = new OrSpec();

                for (TestClassSpec classSpec : testClassSpec) {
                    anyOfTheClasses.addConfiguredTestClass(classSpec);
                }

                final TestDetector classesAndMethodsTestDetector = BooleanSpec.buildTestDetectorFor(Collections.singletonList(anyOfTheClasses));
                aggregatedDetector.addDetector(classesAndMethodsTestDetector);
            }
            return aggregatedDetector;
        } catch (CloverException e) {
            return new DefaultTestDetector();
        }
    }

    @Override
    public String toString() {
        return clover.com.google.common.base.MoreObjects.toStringHelper(this)
                .add("includePattern", includePattern)
                .add("excludePattern", excludePattern)
                .add("testClassSpec", testClassSpec)
                .toString();
    }
}
