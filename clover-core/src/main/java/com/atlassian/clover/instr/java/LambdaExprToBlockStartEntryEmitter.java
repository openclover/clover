package com.atlassian.clover.instr.java;

import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.spi.lang.LanguageConstruct;

/**
 * Code emitter for lambda expressions declared as an expression to be translated into curly braces. Registered at lambda start
 * registers lambda enter method.
 */
public class LambdaExprToBlockStartEntryEmitter extends Emitter {

    protected final MethodSignature signature;
    protected FullMethodInfo method;

    public LambdaExprToBlockStartEntryEmitter(MethodSignature signature, int startLine, int startColumn) {
        super(startLine, startColumn);
        this.signature = signature;
    }

    @Override
    protected void init(InstrumentationState state) {
        if (state.isInstrEnabled()) {
            state.setDirty();
            method = ((FullMethodInfo) state.getSession().enterMethod(getElementContext(),
                    new FixedSourceRegion(getLine(), getColumn()),
                    signature, false, null, true,
                    FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, LanguageConstruct.Builtin.METHOD));
        }
    }
}
