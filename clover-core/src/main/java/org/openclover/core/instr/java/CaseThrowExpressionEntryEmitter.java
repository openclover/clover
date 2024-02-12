package org.openclover.core.instr.java;

import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.spi.lang.LanguageConstruct;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$inc;

/**
 * A code emitter for lambda case expressions with the {@code throw} keyword.
 * Rewrites an expression like:
 * <pre>
 *    case 0 -> throw new Exception();
 *              ^                    ^
 * </pre>
 * into:
 * <pre>
 *    case 0 -> { R.inc(); throw new Exception();
 *              ~~~~~~~~~~ ^                    ^
 * </pre>
 *
 * {@link CaseThrowExpressionExitEmitter}
 */
public class CaseThrowExpressionEntryEmitter extends Emitter {

    /** A line number where expression ends (position of a semicolon) */
    private final int endLine;

    /** A column number where expression ends (position of a semicolon) */
    private final int endCol;

    /**
     * A complexity of the entire expression.
     * It can be non-0 if a ternary expression or another switch expression is present in it.
     */
    private final int complexity;

    /**
     * A statement registered for this case throw expression.
     */
    FullStatementInfo stmtInfo;

    public CaseThrowExpressionEntryEmitter(ContextSet context, int line, int column, int endLine, int endCol, int complexity) {
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
            stmtInfo =
                    state.getSession().addStatement(
                            getElementContext(),
                            new FixedSourceRegion(getLine(), getColumn(), endLine, endCol),
                            complexity,
                            LanguageConstruct.Builtin.STATEMENT);

            if (state.getCfg().isClassInstrStrategy()) {
                // emit text like [{__CLRxxxxxxxx.inc(123);]
                final String instr = "{" +
                        $CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(stmtInfo.getDataIndex())) +
                        ";";
                setInstr(instr);
            }
        }
    }


}
