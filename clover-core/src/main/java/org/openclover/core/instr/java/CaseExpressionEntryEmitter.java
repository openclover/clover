package org.openclover.core.instr.java;

import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.context.NamedContext;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.spi.lang.LanguageConstruct;

/**
 * A code emitter for lambda case expressions. Rewrites an expression like:
 * <pre>
 *    case 0 -> "abc";
 *              ^    ^
 * </pre>
 * into:
 * <pre>
 *    case 0 -> caseInc(123, "abc";
 *              ~~~~~~~~~~~~ ^    ^
 * </pre>
 * <p>
 * {@link CaseExpressionExitEmitter}
 */
public class CaseExpressionEntryEmitter extends Emitter {

    /**
     * A line number where expression ends (position of a semicolon)
     */
    private final int endLine;

    /**
     * A column number where expression ends (position of a semicolon)
     */
    private final int endCol;

    /**
     * A complexity of the entire expression.
     * It can be non-0 if a ternary expression or another switch expression is present in it.
     */
    private final int complexity;

    /**
     * A statement registered for this case expression.
     */
    FullStatementInfo stmtInfo;

    public CaseExpressionEntryEmitter(ContextSet context, int line, int column, int endLine, int endCol, int complexity) {
        super(context, line, column);
        this.endLine = endLine;
        this.endCol = endCol;
        this.complexity = complexity;
    }

    @Override
    protected void init(InstrumentationState state) {
        if (state.isInstrEnabled()) {
            state.setDirty();

            // record the statement
            stmtInfo = state.getSession().addStatement(
                    getElementContext(),
                    new FixedSourceRegion(getLine(), getColumn(), endLine, endCol),
                    complexity,
                    LanguageConstruct.Builtin.STATEMENT);

            boolean classInstrStrategy = state.getCfg().isClassInstrStrategy();
            if (classInstrStrategy) {
                // emit text like [__CLRxxxxxxxx.caseInc(123,]
                final String recorderBase = state.getRecorderPrefix().substring(0, state.getRecorderPrefix().lastIndexOf('.'));
                final StringBuilder instr = new StringBuilder();
                instr.append(recorderBase);
                instr.append(".");
                instr.append(RecorderInstrEmitter.CASE_INC_METHOD);
                instr.append("(");
                instr.append(stmtInfo.getDataIndex());
                // add a comma, because we'll have original case expression as a second argument
                instr.append(",");
                setInstr(instr.toString());
            }
        }
    }

    @Override
    public void addContext(NamedContext ctx) {
        super.addContext(ctx);
        if (stmtInfo != null) {
            stmtInfo.addContext(ctx);
        }
    }

}
