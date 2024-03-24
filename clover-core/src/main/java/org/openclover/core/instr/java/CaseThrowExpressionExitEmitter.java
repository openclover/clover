package org.openclover.core.instr.java;

/**
 * A code emitter for lambda case expressions with the {@code throw} keyword.
 * Rewrites an expression like:
 * <pre>
 *    case 0 -> throw new Exception();
 *              ^                    ^
 * </pre>
 * into:
 * <pre>
 *    case 0 -> throw new Exception();}
 *              ^                    ^~
 * </pre>
 *
 * {@link CaseThrowExpressionEntryEmitter}
 */
public class CaseThrowExpressionExitEmitter extends Emitter {

    private CaseThrowExpressionEntryEmitter entryEmitter;

    public CaseThrowExpressionExitEmitter(CaseThrowExpressionEntryEmitter entryEmitter) {
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
