package org.openclover.core.instr.java;

import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.context.MethodRegexpContext;
import org.openclover.core.context.NamedContext;
import org.openclover.core.registry.entities.MethodSignature;

import java.lang.reflect.Modifier;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$globalSliceStart;
import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$inc;

public class MethodEntryInstrEmitter extends Emitter {
    private final MethodRegistrationNode methodNode;
    private boolean addTestInstr;
    private boolean needsFinally = false;
    /**
     * When true, the 'try{' opening the try-finally block is not emitted here but by a separate
     * {@link MethodTryBlockStartEmitter}. Used for constructors in Java 25+, where the entry inc() is
     * placed before an explicit super()/this() invocation, which must not be enclosed in a try block.
     */
    private boolean tryBlockStartDeferred = false;

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
                && !methodNode.getSignature().isConstructorLike()
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
                if (!tryBlockStartDeferred) {
                    instr.append("try{");
                }
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

    public void setTryBlockStartDeferred(boolean tryBlockStartDeferred) {
        this.tryBlockStartDeferred = tryBlockStartDeferred;
    }

    public boolean needsFinally() {
        return this.needsFinally;
    }
}
