package org.openclover.core.instr.java

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.openclover.core.instr.java.JavaTokenTypes.ASSIGN
import static org.openclover.core.instr.java.JavaTokenTypes.EQUAL
import static org.openclover.core.instr.java.JavaTokenTypes.IDENT
import static org.openclover.core.instr.java.JavaTokenTypes.INT_LITERAL
import static org.openclover.core.instr.java.JavaTokenTypes.LE
import static org.openclover.core.instr.java.JavaTokenTypes.LOR

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