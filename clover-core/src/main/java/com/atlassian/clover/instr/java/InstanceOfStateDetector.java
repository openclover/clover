package com.atlassian.clover.instr.java;

/**
 * Reads subsequent tokens of the expression and detects format of the instanceof expression. Examples:
 * <pre>
 *   obj instanceof String
 *   obj instanceof String str
 * </pre>
 */
public class InstanceOfStateDetector implements CloverTokenConsumer {
    private InstanceOfState state = InstanceOfState.NOTHING;

    @Override
    public void accept(CloverToken token) {
        state = state.nextToken(token);
    }

    /**
     * Whether instanceof expression contains pattern matching variable, e.g.
     * <pre>o instanceof String s</pre>
     */
    public boolean hasVariableDeclaration() {
        return state == InstanceOfState.VARIABLE;
    }
}
