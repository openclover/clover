package com.atlassian.clover.instr.java

import org.junit.Test

import static com.atlassian.clover.instr.java.JavaTokenTypes.BOR
import static com.atlassian.clover.instr.java.JavaTokenTypes.EQUAL
import static com.atlassian.clover.instr.java.JavaTokenTypes.IDENT
import static com.atlassian.clover.instr.java.JavaTokenTypes.INT_LITERAL
import static com.atlassian.clover.instr.java.JavaTokenTypes.LAND
import static com.atlassian.clover.instr.java.JavaTokenTypes.LOR
import static com.atlassian.clover.instr.java.JavaTokenTypes.LPAREN
import static com.atlassian.clover.instr.java.JavaTokenTypes.RPAREN
import static org.junit.Assert.assertEquals

class ExpressionComplexityCounterTest {

    @Test
    void complexityOfOne() {
        ExpressionComplexityCounter counter = new ExpressionComplexityCounter(1)

        // "a == 5 | 12" - bit OR
        counter.accept(new CloverToken(IDENT, "a"))
        counter.accept(new CloverToken(EQUAL, "=="))
        counter.accept(new CloverToken(INT_LITERAL, "5"))
        counter.accept(new CloverToken(BOR, "|"))
        counter.accept(new CloverToken(INT_LITERAL, "12"))

        assertEquals(1, counter.getComplexity())
    }

    @Test
    void complexityOfThree() {
        ExpressionComplexityCounter counter = new ExpressionComplexityCounter(1)

        // "a || (b && c)" - bit OR
        counter.accept(new CloverToken(IDENT, "a"))
        counter.accept(new CloverToken(LOR, "||"))
        counter.accept(new CloverToken(LPAREN, "("))
        counter.accept(new CloverToken(IDENT, "b"))
        counter.accept(new CloverToken(LAND, "&&"))
        counter.accept(new CloverToken(IDENT, "c"))
        counter.accept(new CloverToken(RPAREN, ")"))

        assertEquals(3, counter.getComplexity())
    }

}