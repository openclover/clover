package org.openclover.core.instr.java;

/**
 * A code emitter for lambda case expressions. Rewrites an expression like:
 * <pre>
 *    case 0 -> doSomething();
 *              ^            ^
 * </pre>
 * into:
 * <pre>
 *    case 0 -> doSomething();}
 *              ^            ^~
 * </pre>
 *
 * {@link CaseExpressionEntryEmitter}
 */
public class CaseExpressionExitEmitter extends Emitter {

    private CaseExpressionEntryEmitter entryEmitter;

    public CaseExpressionExitEmitter(CaseExpressionEntryEmitter entryEmitter) {
        this.entryEmitter = entryEmitter;
    }

    @Override
    protected void init(InstrumentationState state) {
        // we must close the wrapped expression only if the start was wrapped, ignoring
        // any CLOVER:OFF inside (state.isInstrEnabled() check would be wrong)
        if (entryEmitter.stmtInfo != null) {
            setInstr("}");
        }
    }
}
