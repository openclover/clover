package com.atlassian.clover.instr.java;

import com.atlassian.clover.instr.Bindings;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.spi.lang.LanguageConstruct;

/**
 * Code emitter for lambda expressions declared as a code block in curly braces.
 * Emits code for the opening brace.
 */
public class LambdaBlockEntryEmitter extends Emitter {
    private MethodSignature signature;
    FullMethodInfo method;

    public LambdaBlockEntryEmitter(MethodSignature signature, int startLine, int startColumn) {
        super(startLine, startColumn);
        this.signature = signature;
    }

    @Override
    protected void init(InstrumentationState state) {
        if (state.isInstrEnabled()) {
            state.setDirty();
            method = (FullMethodInfo) state.getSession().enterMethod(getElementContext(),
                    new FixedSourceRegion(getLine(), getColumn()),
                    signature, false, null, true,
                    FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, LanguageConstruct.Builtin.METHOD);

            StringBuilder instr = new StringBuilder();
            instr.append(Bindings.$CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(method.getDataIndex())));
            instr.append(";");
            setInstr(instr.toString());
        }
    }

}
