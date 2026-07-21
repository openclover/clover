package org.openclover.core.instr.java;

/**
 * Reads subsequent tokens of the expression and detects format of the instanceof expression. Examples:
 * <pre>
 *   obj instanceof String
 *   obj instanceof String str
 *   obj instanceof final String str
 *   obj instanceof int[] array
 *   obj instanceof Point(int x, int y)
 * </pre>
 */
public class InstanceOfStateDetector implements CloverTokenConsumer {
    private InstanceOfState state = InstanceOfState.NOTHING;

    @Override
    public void accept(CloverToken token) {
        state = state.nextToken(token);
    }

    /**
     * Whether the instanceof expression introduces a pattern binding - either a type pattern
     * variable, e.g. <pre>o instanceof String s</pre> or a record deconstruction pattern, e.g.
     * <pre>o instanceof Point(int x, int y)</pre>. Such expressions must not be branch-instrumented.
     */
    public boolean hasPatternBinding() {
        return state == InstanceOfState.VARIABLE
                || state == InstanceOfState.RECORD_DECONSTRUCTION;
    }
}
