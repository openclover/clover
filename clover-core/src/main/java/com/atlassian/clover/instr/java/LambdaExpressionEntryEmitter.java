package com.atlassian.clover.instr.java;

import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import org.jetbrains.annotations.NotNull;

/**
 * Code emitter for lambdas declared as an expression. It wraps such expression into a method argument.
 * Emits code for the beginning of an expression.
 */
public class LambdaExpressionEntryEmitter extends Emitter {
    private final MethodSignature lambdaSignature;
    private final String classCast;
    FullMethodInfo method;
    private int bodyStartLine, bodyStartColumn;

    /**
     *
     * <pre>
     *   (Integer)(x, y) -> x + y
     *            ^ startLine,startColumn
     *                      ^ bodyStartLine,bodyStartColumn
     *   ^^^^^^^^ classCast
     * </pre>
     *
     * @param lambdaSignature signature of the method which will be registered for this lambda
     * @param classCast optional class cast to be put just before lambda
     * @param startLine    line number where argument list starts
     * @param startColumn  column number where argument list starts
     * @param bodyStartLine    line number where expression body starts
     * @param bodyStartColumn  column number where expression body starts
     */
    public LambdaExpressionEntryEmitter(@NotNull final MethodSignature lambdaSignature,
                                        @NotNull final String classCast,
                                        int startLine, int startColumn,
                                        int bodyStartLine, int bodyStartColumn) {
        super(startLine, startColumn);
        this.lambdaSignature = lambdaSignature;
        this.classCast = classCast;
        this.bodyStartLine = bodyStartLine;
        this.bodyStartColumn = bodyStartColumn;
    }

    @Override
    protected void init(@NotNull final InstrumentationState state) {
        if (state.isInstrEnabled()) {
            state.setDirty();
            method = (FullMethodInfo) state.getSession().enterMethod(
                    getElementContext(),
                    new FixedSourceRegion(getLine(), getColumn()),
                    lambdaSignature, false, null, true,
                    FullMethodInfo.DEFAULT_METHOD_COMPLEXITY, LanguageConstruct.Builtin.METHOD);

            // wrap lambda only in class instrumentation strategy (only then lambdaInc() is defined)
            boolean classInstrStrategy = state.getCfg().isClassInstrStrategy();
            if (classInstrStrategy) {
                // emit text like [__CLRxxxxxxxx.lambdaInc(123, ]
                final String recorderBase = state.getRecorderPrefix().substring(0, state.getRecorderPrefix().lastIndexOf('.'));
                final StringBuilder instr = new StringBuilder();
                instr.append(recorderBase);
                instr.append(".");
                instr.append(RecorderInstrEmitter.LAMBDA_INC_METHOD);
                instr.append("(");
                instr.append(method.getDataIndex());
                instr.append(","); // add a comma because we'll have original lambda as a second argument of lambdaInc
                instr.append(classCast); // add a class cast before lambda (optional)
                setInstr(instr.toString());
            }
        }
    }

    public int getBodyStartLine() {
        return bodyStartLine;
    }

    public int getBodyStartColumn() {
        return bodyStartColumn;
    }
}
