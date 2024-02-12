package org.openclover.core.instr.java;

/**
 * Reads subsequent tokens and counts opening and closing parenthesis.
 * Used to figure out depth of () nesting and if we reached the last one.
 */
public class ParenthesisCounter implements CloverTokenConsumer {
    private int openParens;

    public ParenthesisCounter(int openParens) {
        this.openParens = openParens;
    }

    @Override
    public void accept(CloverToken token) {
        if (token.getType() == JavaTokenTypes.LPAREN) {
            openParens++;
        } else if (token.getType() == JavaTokenTypes.RPAREN) {
            openParens--;
        }
    }

    /**
     * True if we did not reach the last closing parenthesis.
     */
    public boolean notLastParenthesis() {
        return openParens > 0;
    }
}
