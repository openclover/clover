package com.atlassian.clover.instr.java;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$globalSliceStart;
import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$inc;

import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.registry.entities.MethodSignature;

import java.lang.reflect.Modifier;

public class MethodEntryInstrEmitter extends Emitter {
    private MethodRegistrationNode methodNode;
    private boolean addTestInstr;
    private boolean needsFinally = false;

    public MethodEntryInstrEmitter(MethodRegistrationNode node) {
        this.methodNode = node;
    }

    @Override
    protected boolean acceptsContextType(NamedContext context) {
        return context instanceof MethodRegexpContext;
    }

    @Override
    public void init(InstrumentationState state) {
        addTestInstr =
            !state.getCfg().isRecordTestResults() // if recording test results, we need to rewrite the tests which occurs external to the method
                && state.isDetectTests()
                && state.getTestDetector().isMethodMatch(state, JavaMethodContext.createFor(methodNode.getSignature()));
        StringBuilder instr = new StringBuilder();

        if (state.isInstrEnabled()) {
            state.setDirty();
            if (addTestInstr) {
                instr.append("try{");

                String typeInstr = "getClass().getName()";
                if (Modifier.isStatic(getSignature().getBaseModifiersMask())) {
                   typeInstr = getMethod().getContainingClass().getName() + ".class.getName()";
                }

                instr.append($CoverageRecorder$globalSliceStart(state.getRecorderPrefix(), typeInstr, Integer.toString(methodNode.getMethod().getDataIndex()))).append(";");
                needsFinally = true;
            }
            else if (state.getCfg().isIntervalBasedFlushing()) {
                instr.append("try{");
                needsFinally = true;
            }

            instr.append($CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(methodNode.getMethod().getDataIndex())));
            instr.append(";");
        }
        setInstr(instr.toString());
    }

    public boolean isAddTestInstr() {
        return addTestInstr;
    }

    public MethodSignature getSignature() {
        return methodNode.getSignature();
    }

    public MethodInfo getMethod() {
        return methodNode.getMethod();
    }

    public boolean needsFinally() {
        return this.needsFinally;
    }
}
