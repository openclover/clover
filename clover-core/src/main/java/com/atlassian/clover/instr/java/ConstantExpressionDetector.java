package com.atlassian.clover.instr.java;

import java.util.BitSet;

/**
 * Reads subsequent tokens and recognizes if this is a "pure" constant expression.
 */
public class ConstantExpressionDetector implements CloverTokenConsumer {

    // all the tokens that can be part of a constant expression.
    private static final BitSet CONSTANT_TOKENS = new BitSet();

    static {
        CONSTANT_TOKENS.set(JavaTokenTypes.LITERAL_true);
        CONSTANT_TOKENS.set(JavaTokenTypes.LITERAL_false);
        CONSTANT_TOKENS.set(JavaTokenTypes.LITERAL_null);

        CONSTANT_TOKENS.set(JavaTokenTypes.NUM_INT);
        CONSTANT_TOKENS.set(JavaTokenTypes.NUM_FLOAT);
        CONSTANT_TOKENS.set(JavaTokenTypes.NUM_LONG);
        CONSTANT_TOKENS.set(JavaTokenTypes.NUM_DOUBLE);
        CONSTANT_TOKENS.set(JavaTokenTypes.CHAR_LITERAL);
        CONSTANT_TOKENS.set(JavaTokenTypes.STRING_LITERAL);

        CONSTANT_TOKENS.set(JavaTokenTypes.PLUS);
        CONSTANT_TOKENS.set(JavaTokenTypes.MINUS);
        CONSTANT_TOKENS.set(JavaTokenTypes.DIV);
        CONSTANT_TOKENS.set(JavaTokenTypes.MOD);
        CONSTANT_TOKENS.set(JavaTokenTypes.STAR);

        CONSTANT_TOKENS.set(JavaTokenTypes.NOT_EQUAL);
        CONSTANT_TOKENS.set(JavaTokenTypes.EQUAL);
        CONSTANT_TOKENS.set(JavaTokenTypes.LNOT);
        CONSTANT_TOKENS.set(JavaTokenTypes.LOR);
        CONSTANT_TOKENS.set(JavaTokenTypes.LAND);

        CONSTANT_TOKENS.set(JavaTokenTypes.BOR);
        CONSTANT_TOKENS.set(JavaTokenTypes.BAND);
        CONSTANT_TOKENS.set(JavaTokenTypes.BNOT);
        CONSTANT_TOKENS.set(JavaTokenTypes.BXOR);

        CONSTANT_TOKENS.set(JavaTokenTypes.LPAREN);
        CONSTANT_TOKENS.set(JavaTokenTypes.RPAREN);
        CONSTANT_TOKENS.set(JavaTokenTypes.QUESTION);
        CONSTANT_TOKENS.set(JavaTokenTypes.COLON);
        CONSTANT_TOKENS.set(JavaTokenTypes.COMMA);

        CONSTANT_TOKENS.set(JavaTokenTypes.GT);
        CONSTANT_TOKENS.set(JavaTokenTypes.LT);
        CONSTANT_TOKENS.set(JavaTokenTypes.GE);
        CONSTANT_TOKENS.set(JavaTokenTypes.LE);

        CONSTANT_TOKENS.set(JavaTokenTypes.SL);
        CONSTANT_TOKENS.set(JavaTokenTypes.SR);
        CONSTANT_TOKENS.set(JavaTokenTypes.BSR);
    }

    private boolean constant = true;
    @Override
    public void accept(CloverToken token) {
        if (!CONSTANT_TOKENS.get(token.getType())) {
            constant = false;
        }
    }

    public boolean isConstant() {
        return constant;
    }
}
