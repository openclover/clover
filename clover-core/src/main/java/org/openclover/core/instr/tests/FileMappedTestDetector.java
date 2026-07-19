package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;


public class FileMappedTestDetector implements TestDetector {

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

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeInt(testFileMatchers.size());
        for (TestSourceMatcher matcher : testFileMatchers) {
            TestDetectorIO.writeMatcher(out, matcher);
        }
        TestDetectorIO.writeDetector(out, defaultDetector);
    }

    public static FileMappedTestDetector read(TaggedDataInput in) throws IOException {
        final FileMappedTestDetector detector = new FileMappedTestDetector();
        final int count = in.readInt();
        for (int i = 0; i < count; i++) {
            detector.addTestSourceMatcher(in.read(TestSourceMatcher.class));
        }
        detector.defaultDetector = in.read(TestDetector.class);
        return detector;
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
