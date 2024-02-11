package org.openclover.core.instr.java;

import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.spi.lang.LanguageConstruct;

/**
 * Code emitter for lambdas declared as an expression. It wraps such expression into a method argument.
 * Emits code for the end of expression.
 */
public class LambdaExpressionExitEmitter extends Emitter {
    private LambdaExpressionEntryEmitter entryEmitter;

    public LambdaExpressionExitEmitter(LambdaExpressionEntryEmitter entryEmitter, int endLine, int endColumn) {
        super(endLine, endColumn);
        this.entryEmitter = entryEmitter;
    }

    @Override
    protected void init(InstrumentationState state) {
        // we shall check "if (state.isInstrEnabled())" but as CLOVER:OFF could have been written in the middle
        // therefore we check for non-null value of a method field -> it means that enterMethod() was called
        if (entryEmitter.method != null) {
            // treat expression inside this lambda as statement with complexity 0 and record it in the database
            final FullStatementInfo statementInfo = state.getSession().addStatement(
                    getElementContext(),
                    new FixedSourceRegion(entryEmitter.getBodyStartLine(), entryEmitter.getBodyStartColumn(),
                            this.getLine(), this.getColumn()),
                    0, LanguageConstruct.Builtin.STATEMENT);

            // close the method
            state.getSession().exitMethod(getLine(), getColumn());

            // wrap lambda only in class instrumentation strategy
            boolean classInstrStrategy = state.getCfg().isClassInstrStrategy();
            if (classInstrStrategy) {
                // append statement index after the lambda call and
                // write closing brace for argument list of a lambdaInc wrapper
                setInstr("," + statementInfo.getDataIndex() + ")");
            }
        }
    }
}
