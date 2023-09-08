package com.atlassian.clover.instr.tests;

import java.util.List;

import static org.openclover.util.Lists.newArrayList;

/**
 * A TestDetector which has many TestDetectors and applies a boolean strategy for joining them together.
 */
public class AggregateTestDetector implements TestDetector {

    private final List<TestDetector> detectors = newArrayList();
    private final BooleanStrategy strategy;


    public AggregateTestDetector(BooleanStrategy strategy) {
        this.strategy = strategy;
    }

    public void addDetector(TestDetector detector) {
        detectors.add(detector);
    }
    
    public boolean isEmpty() {
        return detectors.isEmpty();
    }

    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        boolean[] values = new boolean[detectors.size()];
        for (int i = 0; i < detectors.size(); i++) {
            TestDetector detector = detectors.get(i);
            values[i] = detector.isTypeMatch(sourceContext, typeContext);
        }
        return strategy.process(values);
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        boolean[] values = new boolean[detectors.size()];        
        for (int i = 0; i < detectors.size(); i++) {
            TestDetector detector = detectors.get(i);
            values[i] = detector.isMethodMatch(sourceContext, methodContext);
        }
        return strategy.process(values);
    }
}
