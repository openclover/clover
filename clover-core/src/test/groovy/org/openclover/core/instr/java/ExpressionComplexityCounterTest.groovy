package org.openclover.core.instr.java

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.openclover.core.instr.java.JavaTokenTypes.BOR
import static org.openclover.core.instr.java.JavaTokenTypes.COLON
import static org.openclover.core.instr.java.JavaTokenTypes.EQUAL
import static org.openclover.core.instr.java.JavaTokenTypes.IDENT
import static org.openclover.core.instr.java.JavaTokenTypes.INT_LITERAL
import static org.openclover.core.instr.java.JavaTokenTypes.LAND
import static org.openclover.core.instr.java.JavaTokenTypes.LOR
import static org.openclover.core.instr.java.JavaTokenTypes.LPAREN
import static org.openclover.core.instr.java.JavaTokenTypes.LT
import static org.openclover.core.instr.java.JavaTokenTypes.MINUS
import static org.openclover.core.instr.java.JavaTokenTypes.QUESTION
import static org.openclover.core.instr.java.JavaTokenTypes.RPAREN

class ExpressionComplexityCounterTest {

    @Test
    void complexityOfOne() {
        ExpressionComplexityCounter counter = new ExpressionComplexityCounter(1)

        // "a == 5 | 12" - bit OR is not a branch
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

        // "a || (b && c)" - logical AND / OR are branches
        counter.accept(new CloverToken(IDENT, "a"))
        counter.accept(new CloverToken(LOR, "||"))
        counter.accept(new CloverToken(LPAREN, "("))
        counter.accept(new CloverToken(IDENT, "b"))
        counter.accept(new CloverToken(LAND, "&&"))
        counter.accept(new CloverToken(IDENT, "c"))
        counter.accept(new CloverToken(RPAREN, ")"))

        assertEquals(3, counter.getComplexity())
    }

    @Test
    void complexityOfTernaryOperator() {
        ExpressionComplexityCounter counter = new ExpressionComplexityCounter(1)

        // "a < 0 ? -1 : 1" - ternary has two branches so one cycle
        counter.accept(new CloverToken(IDENT, "a"))
        counter.accept(new CloverToken(LT, "<"))
        counter.accept(new CloverToken(INT_LITERAL, "0"))
        counter.accept(new CloverToken(QUESTION, "?"))
        counter.accept(new CloverToken(MINUS, "-"))
        counter.accept(new CloverToken(INT_LITERAL, "1"))
        counter.accept(new CloverToken(COLON, ":"))
        counter.accept(new CloverToken(INT_LITERAL, "1"))

        assertEquals(1, counter.getComplexity())
    }

}