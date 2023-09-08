package com.atlassian.clover.cmdline;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.instr.tests.AggregateTestDetector;
import com.atlassian.clover.instr.tests.AndStrategy;
import com.atlassian.clover.instr.tests.AntPatternTestDetectorFilter;
import com.atlassian.clover.instr.tests.DefaultTestDetector;
import com.atlassian.clover.instr.tests.OrStrategy;
import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.spec.instr.test.BooleanSpec;
import com.atlassian.clover.spec.instr.test.TestClassSpec;
import com.atlassian.clover.spec.instr.test.TestMethodSpec;
import org.openclover.util.Objects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileClassMethodTestDetector implements TestDetector {

    // figure out includes and excludes
    private TestDetector testDetector;

    private String root;
    private String includePattern;
    private String excludePattern;
    private List<TestClassSpec> testClassSpec = new ArrayList<>();


    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return getTestDetector().isTypeMatch(sourceContext, typeContext);
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return getTestDetector().isMethodMatch(sourceContext, methodContext);
    }

    public void setRoot(String root) {
        this.root = root;
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
     * Add TestMethodSpec to the last TestClassSpec.
     * In case there's no TestClassSpec then create new one accepting any class.
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
                    root == null ? new File("") : new File(root),
                    includePattern == null ? null : includePattern.split(","),
                    excludePattern == null ? null : excludePattern.split(","));

            // we must match all of: files, classes, methods
            final AggregateTestDetector aggregatedDetector = new AggregateTestDetector(new AndStrategy());
            aggregatedDetector.addDetector(includesExcludesTestDetector);

            // add a non-empty detector only, otherwise it will reject everything
            if (!testClassSpec.isEmpty()) {
                final AggregateTestDetector anyOfTheClasses = new AggregateTestDetector(new OrStrategy());
                BooleanSpec.buildTestDetectorFor(anyOfTheClasses, testClassSpec);
                aggregatedDetector.addDetector(anyOfTheClasses);
            }

            return aggregatedDetector;
        } catch (CloverException e) {
            return new DefaultTestDetector();
        }
    }

    @Override
    public String toString() {
        return Objects.toStringBuilder(this)
                .add("root", root)
                .add("includePattern", includePattern)
                .add("excludePattern", excludePattern)
                .add("testClassSpec", testClassSpec)
                .toString();
    }
}
