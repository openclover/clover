package com.atlassian.clover.instr.tests;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;


public class FileMappedTestDetector implements TestDetector, Serializable {

    private List<TestSourceMatcher> testFileMatchers = newArrayList();

    private TestDetector defaultDetector = null;

    public FileMappedTestDetector() {
    }

    public FileMappedTestDetector(TestDetector defaultDetector) {
        this.defaultDetector = defaultDetector;
    }

    public void addTestSourceMatcher(TestSourceMatcher matcher) {
        testFileMatchers.add(matcher);
    }

    public TestDetector getDetectorForFile(File f) {
        // <TestDetector>
        AggregateTestDetector detectors = new AggregateTestDetector(new OrStrategy());
        for (TestSourceMatcher matcher : testFileMatchers) {
            if (matcher.matchesFile(f)) {
                detectors.addDetector(matcher.getDetector());
            }
        }
        return detectors.isEmpty() ? defaultDetector : detectors;
    }

    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        TestDetector t = getDetectorForFile(sourceContext.getSourceFile());
        return t != null && t.isTypeMatch(sourceContext, typeContext);
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        TestDetector t = getDetectorForFile(sourceContext.getSourceFile());
        return t != null && t.isMethodMatch(sourceContext, methodContext);
    }
}
