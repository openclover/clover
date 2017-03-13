package com.atlassian.clover.instr.java;

import java.util.BitSet;

/**
 * a class for processing expressions and gathering information about them
 */
public class ExpressionInfo {

    private boolean constant = false;
    private boolean containsAssign = false;
    private int complexity = 0;

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


    public static ExpressionInfo fromTokens(CloverToken start, CloverToken end) {

        ExpressionInfo info = new ExpressionInfo();


        info.constant = true;
        for (CloverToken t = start; t.getNext() != end.getNext(); t=t.getNext()) {
            if (!CONSTANT_TOKENS.get(t.getType())) {
                info.constant = false;
                break;
            }
        }
        

        if (!info.constant) {
            info.complexity = 1; // any expression reaching here represents a "branch" 
            int openParens = 1;
            CloverToken curr = start;
            while (curr != null  && openParens > 0 && curr != end) {
                switch (curr.getType()) {
                    case JavaTokenTypes.ASSIGN:
                        info.containsAssign = true;
                        break;
                    case JavaTokenTypes.LPAREN:
                        openParens++;
                        break;
                    case JavaTokenTypes.RPAREN:
                        openParens--;
                        break;
                    case JavaTokenTypes.LOR:
                    case JavaTokenTypes.LAND:
                        info.complexity++;
                        break;
                    default:
                        break;
                }
                curr = curr.getNext();

            }
        }
        return info;
    }

    public boolean isConstant() {
        return constant;
    }

    public boolean isContainsAssign() {
        return containsAssign;
    }

    public boolean isInstrumentable() {
        return !constant && !containsAssign;
    }

    public int getComplexity() {
        return complexity;
    }
}
