package com.atlassian.clover.instr.tests;

public class NoTestDetector implements TestDetector {
    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return false;
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return false;
    }
}
