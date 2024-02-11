package org.openclover.core.instr.java;

import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.registry.entities.MethodSignature;

public class JavaMethodContext implements TestDetector.MethodContext {
    public static JavaMethodContext createFor(MethodSignature signature) {
        return new JavaMethodContext(signature);
    }

    private MethodSignature signature;

    private JavaMethodContext(MethodSignature signature) {
        this.signature = signature;
    }

    @Override
    public MethodSignature getSignature() {
        return signature;
    }
}
