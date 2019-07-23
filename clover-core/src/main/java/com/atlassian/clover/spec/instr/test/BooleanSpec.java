package com.atlassian.clover.spec.instr.test;

import com.atlassian.clover.instr.tests.AggregateTestDetector;
import com.atlassian.clover.instr.tests.BooleanStrategy;
import com.atlassian.clover.instr.tests.DefaultTestDetector;
import com.atlassian.clover.instr.tests.OrStrategy;
import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.instr.tests.TestSpec;
import com.atlassian.clover.api.CloverException;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static clover.com.google.common.collect.Lists.newArrayList;

public abstract class BooleanSpec implements Serializable {
    private List<TestClassSpec> testClassSpecs = null;

    public void addConfiguredTestClass(final TestClassSpec testClassSpec) {
        if (this.testClassSpecs == null) {
            this.testClassSpecs = newArrayList();
        }
        this.testClassSpecs.add(testClassSpec);
    }

    public List<TestClassSpec> getTestClassSpecs() {
        return testClassSpecs;
    }

    public abstract BooleanStrategy getStrategy();

    public static TestDetector buildTestDetectorFor(final List<BooleanSpec> boolSpecs) throws CloverException {
        if (boolSpecs != null) {
            final AggregateTestDetector testDetectorGroups = new AggregateTestDetector(new OrStrategy());
            for (final BooleanSpec booleanSpec : boolSpecs) {

                if (booleanSpec.getTestClassSpecs() != null) {
                    final AggregateTestDetector testDetectors = new AggregateTestDetector(booleanSpec.getStrategy());
                    buildTestDetectorFor(testDetectors, booleanSpec.getTestClassSpecs());
                    testDetectorGroups.addDetector(testDetectors);
                }
            }
            return testDetectorGroups;
        } else {
            return new DefaultTestDetector();
        }
    }

    public static void buildTestDetectorFor(final AggregateTestDetector testDetectors,
                                            final List<TestClassSpec> testClassSpecs) throws CloverException {

        for (final TestClassSpec testClassSpec : testClassSpecs) {
            final TestSpec testSpec = new TestSpec();
            try {
                if (testClassSpec.getPackage() != null) {
                    testSpec.setPkgPattern(Pattern.compile(testClassSpec.getPackage()));
                }
                if (testClassSpec.getAnnotation() != null) {
                    testSpec.setClassAnnotationPattern(Pattern.compile(testClassSpec.getAnnotation()));
                }
                if (testClassSpec.getTag() != null) {
                    testSpec.setClassTagPattern(Pattern.compile(testClassSpec.getTag()));
                }
                if (testClassSpec.getName() != null) {
                    testSpec.setClassPattern(Pattern.compile(testClassSpec.getName()));
                }
                if (testClassSpec.getSuper() != null) {
                    testSpec.setSuperPattern(Pattern.compile(testClassSpec.getSuper()));
                }

                final AggregateTestDetector methodDetectors = new AggregateTestDetector(new OrStrategy());
                for (final TestMethodSpec methodSpec : testClassSpec.getTestMethods()) {
                    final TestSpec method = new TestSpec(testSpec);

                    if (methodSpec.getAnnotation() != null) {
                        method.setMethodAnnotationPattern(Pattern.compile(methodSpec.getAnnotation()));
                    }
                    if (methodSpec.getTag() != null) {
                        method.setMethodTagPattern(Pattern.compile(methodSpec.getTag()));
                    }
                    if (methodSpec.getName() != null) {
                        method.setMethodPattern(Pattern.compile(methodSpec.getName()));
                    }
                    if (methodSpec.getReturnType() != null) {
                        method.setMethodReturnTypePattern(Pattern.compile(methodSpec.getReturnType()));
                    }
                    methodDetectors.addDetector(method);
                }
                if (!methodDetectors.isEmpty()) {
                    testDetectors.addDetector(methodDetectors);
                } else {
                    testDetectors.addDetector(testSpec);
                }
            }
            catch (PatternSyntaxException e) {
                throw new CloverException("Error parsing regular expression: " + e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        String content = "testclasses(";
        if (testClassSpecs != null) {
            for (final TestClassSpec testClass : testClassSpecs) {
                content += "\n\t" + testClass.toString();
            }
        }
        content += ")";
        return content;
    }
}
