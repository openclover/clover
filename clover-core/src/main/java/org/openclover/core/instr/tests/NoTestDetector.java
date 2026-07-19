package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;

import java.io.IOException;

public class NoTestDetector implements TestDetector {
    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return false;
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return false;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        // stateless
    }

    public static NoTestDetector read(TaggedDataInput in) throws IOException {
        return new NoTestDetector();
    }
}
