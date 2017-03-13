package com.atlassian.clover.instr.java;

import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.registry.entities.MethodSignature;

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
