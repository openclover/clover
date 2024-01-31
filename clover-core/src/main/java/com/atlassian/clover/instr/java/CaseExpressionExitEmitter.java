package com.atlassian.clover.instr.java;

/**
 * A code emitter for lambda case expressions. Rewrites an expression like:
 * <pre>
 *    case 0 -> "abc";
 *              ^    ^
 * </pre>
 * into:
 * <pre>
 *    case 0 -> "abc");
 *              ^    ~^
 * </pre>
 *
 * {@link CaseExpressionEntryEmitter}
 */
public class CaseExpressionExitEmitter extends Emitter {

    private final CaseExpressionEntryEmitter entryEmitter;

    public CaseExpressionExitEmitter(CaseExpressionEntryEmitter entryEmitter) {
        this.entryEmitter = entryEmitter;
    }

    @Override
    protected void init(InstrumentationState state) {
        // we must close the wrapped expression only if the start was wrapped, ignoring
        // any CLOVER:OFF inside (state.isInstrEnabled() check would be wrong)
        if (entryEmitter.stmtInfo != null && state.getCfg().isClassInstrStrategy()) {
            setInstr(")");
        }
    }
}
