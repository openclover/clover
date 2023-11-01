package com.atlassian.clover.instr.java

import org.junit.Test

import static com.atlassian.clover.instr.java.JavaTokenTypes.ASSIGN
import static com.atlassian.clover.instr.java.JavaTokenTypes.EQUAL
import static com.atlassian.clover.instr.java.JavaTokenTypes.IDENT
import static com.atlassian.clover.instr.java.JavaTokenTypes.INT_LITERAL
import static com.atlassian.clover.instr.java.JavaTokenTypes.LE
import static com.atlassian.clover.instr.java.JavaTokenTypes.LOR
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class AssignmentDetectorTest {

    @Test
    void containsAssign() {
        AssignmentDetector detector = new AssignmentDetector()
        // a = 123
        detector.accept(new CloverToken(IDENT, "a"))
        detector.accept(new CloverToken(ASSIGN, "="))
        detector.accept(new CloverToken(INT_LITERAL, "123"))

        assertTrue(detector.containsAssign())
    }

    @Test
    void doesNotContainAssignment() {
        AssignmentDetector detector = new AssignmentDetector()

        // a <= 123 || b == 0
        detector.accept(new CloverToken(IDENT, "a"))
        detector.accept(new CloverToken(LE, "<="))
        detector.accept(new CloverToken(INT_LITERAL, "123"))
        detector.accept(new CloverToken(LOR, "||"))
        detector.accept(new CloverToken(IDENT, "b"))
        detector.accept(new CloverToken(EQUAL, "=="))
        detector.accept(new CloverToken(INT_LITERAL, "0"))

        assertFalse(detector.containsAssign())
    }
}