package org.openclover.core.instr.java;

/**
 * Reads subsequent tokens from an expression and counts cyclomatic complexity for it.
 * Every "||" or "&amp;&amp;" adds 1 to complexity.
 */
public class ExpressionComplexityCounter implements CloverTokenConsumer {
    private int complexity;

    public ExpressionComplexityCounter(int complexity) {
        this.complexity = complexity;
    }

    @Override
    public void accept(CloverToken token) {
        if (token.getType() == JavaTokenTypes.LOR || token.getType() == JavaTokenTypes.LAND) {
            complexity++;
        }
    }

    public int getComplexity() {
        return complexity;
    }
}
