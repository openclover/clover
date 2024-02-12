package org.openclover.core.instr.java;

import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.MethodSignature;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.runtime.instr.Bindings;

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
