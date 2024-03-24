package org.openclover.core.instr.java;

import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.context.NamedContext;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.spi.lang.LanguageConstruct;

import static org.openclover.runtime.instr.Bindings.$CoverageRecorder$inc;

/**
 * A code emitter for lambda case expressions. In case of expressions returning values (a heuristic is used to
 * determine this), it rewrites:
 * <pre>
 *    case 0 -> "abc";
 *              ^    ^
 * </pre>
 * into:
 * <pre>
 *    case 0 -> { R.inc(123); return "abc";
 *              ~~~~~~~~~~~~~~~~~~~~ ^    ^
 * </pre>
 * <br/>
 * In case of expressions returning void it rewrites:
 * <pre>
 *    case 0 -> println("abc");
 *              ^             ^
 * </pre>
 * into:
 * <pre>
 *    case 0 -> { R.inc(123); println("abc");
 *              ~~~~~~~~~~~~~ ^             ^
 * </pre>
 * <br/>
 * Notes on the heuristic:
 * It's not possible to determine whether a given method returns a value or is void. Thus, we assume
 * that if the switch is used inside an expression (e.g. variable assignment, return value, method call argument),
 * then all case expressions must return a value too. This will not be true if a value of the switch statement
 * is ignored. In such case, a special code comment can be to guide the emitter.
 * <br/>
 * See also: {@link CaseExpressionExitEmitter}
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
     * Whether the case expression is placed inside a switch statement being part of on expression.
     * If yes, then it means that we must add the "return" keyword. Notice that presence of 'CLOVER:RETURN'
     * will also add the "return" keyword, even if isInsideExpression is false.
     */
    private final boolean isInsideExpression;

    /**
     * A statement registered for this case expression.
     */
    FullStatementInfo stmtInfo;

    public CaseExpressionEntryEmitter(ContextSet context, int line, int column, int endLine, int endCol,
                                      int complexity, boolean isInsideExpression) {
        super(context, line, column);
        this.endLine = endLine;
        this.endCol = endCol;
        this.complexity = complexity;
        this.isInsideExpression = isInsideExpression;
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

            // if inside expression or 'CLOVER:RETURN' present then add "return"
            boolean withReturn = isInsideExpression;
            if (state.needsReturnKeyword()) {
                withReturn = true;
                state.setNeedsReturnKeyword(false); // the flag is used only once
            }

            // emit text like [{__CLRxxxxxxxx.inc(123);return] or [{__CLRxxxxxxxx.inc(123);]
            final String instr;
            if (withReturn) {
                instr = "{" + $CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(stmtInfo.getDataIndex())) + ";return ";
            } else {
                instr = "{" + $CoverageRecorder$inc(state.getRecorderPrefix(), Integer.toString(stmtInfo.getDataIndex())) + ";";
            }
            setInstr(instr);
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
