package org.openclover.core.instr.java

import com.atlassian.clover.instr.java.CloverToken
import com.atlassian.clover.instr.java.JavaTokenTypes
import com.atlassian.clover.instr.java.ParenthesisCounter
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ParenthesisCounterTest {

    CloverToken LEFT_PAREN = new CloverToken(JavaTokenTypes.LPAREN, "(")
    CloverToken RIGHT_PAREN = new CloverToken(JavaTokenTypes.RPAREN, ")")

    @Test
    void initialStateWithZeroOpened() {
        ParenthesisCounter counter = new ParenthesisCounter(0)
        assertFalse(counter.notLastParenthesis())
    }

    @Test
    void initialStateWithOneOpened() {
        ParenthesisCounter counter = new ParenthesisCounter(1)
        assertTrue(counter.notLastParenthesis())
    }

    @Test
    void openAndCloseParenthesis() {
        ParenthesisCounter counter = new ParenthesisCounter(0)
        assertFalse(counter.notLastParenthesis())

        // "( )"
        counter.accept(LEFT_PAREN)
        assertTrue(counter.notLastParenthesis())

        counter.accept(RIGHT_PAREN)
        assertFalse(counter.notLastParenthesis())
    }

    @Test
    void nestedParenthesis() {
        ParenthesisCounter counter = new ParenthesisCounter(0)
        assertFalse(counter.notLastParenthesis())

        // "( ( ( ) ) ( ) )"
        //  1 2 3 2 1 2 1 0
        counter.accept(LEFT_PAREN)
        counter.accept(LEFT_PAREN)
        counter.accept(LEFT_PAREN)
        counter.accept(RIGHT_PAREN)
        counter.accept(RIGHT_PAREN)
        assertTrue(counter.notLastParenthesis())

        counter.accept(LEFT_PAREN)
        counter.accept(RIGHT_PAREN)
        assertTrue(counter.notLastParenthesis())

        counter.accept(RIGHT_PAREN)
        assertFalse(counter.notLastParenthesis())
    }
}