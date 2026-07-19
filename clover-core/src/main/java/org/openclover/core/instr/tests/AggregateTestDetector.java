package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;

import java.io.IOException;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

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

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        // strategy stored by name so new BooleanStrategy implementations can be added later
        out.writeUTF(strategy.getClass().getSimpleName());
        out.writeInt(detectors.size());
        for (TestDetector detector : detectors) {
            TestDetectorIO.writeDetector(out, detector);
        }
    }

    public static AggregateTestDetector read(TaggedDataInput in) throws IOException {
        final BooleanStrategy strategy = strategyForName(in.readUTF());
        final AggregateTestDetector aggregate = new AggregateTestDetector(strategy);
        final int count = in.readInt();
        for (int i = 0; i < count; i++) {
            aggregate.addDetector(in.read(TestDetector.class));
        }
        return aggregate;
    }

    private static BooleanStrategy strategyForName(String name) throws IOException {
        if (AndStrategy.class.getSimpleName().equals(name)) {
            return new AndStrategy();
        } else if (OrStrategy.class.getSimpleName().equals(name)) {
            return new OrStrategy();
        }
        throw new IOException("Unknown boolean strategy: " + name);
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
