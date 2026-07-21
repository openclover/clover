package org.openclover.core.instr.java

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.openclover.core.instr.java.JavaTokenTypes.DOT
import static org.openclover.core.instr.java.JavaTokenTypes.IDENT
import static org.openclover.core.instr.java.JavaTokenTypes.INSTANCEOF
import static org.openclover.core.instr.java.JavaTokenTypes.INT
import static org.openclover.core.instr.java.JavaTokenTypes.LBRACK
import static org.openclover.core.instr.java.JavaTokenTypes.LPAREN
import static org.openclover.core.instr.java.JavaTokenTypes.COMMA
import static org.openclover.core.instr.java.JavaTokenTypes.RBRACK
import static org.openclover.core.instr.java.JavaTokenTypes.RPAREN

class InstanceOfStateTest {
    @Test
    void detectSimpleInstanceOf() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof String
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "String"))

        assertEquals(InstanceOfState.FULL_TYPE, state)
    }

    @Test
    void detectArrayTypeInstanceOf() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof String[]
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "String"))
        state = state.nextToken(new CloverToken(LBRACK, "["))
        state = state.nextToken(new CloverToken(RBRACK, "]"))

        assertEquals(InstanceOfState.FULL_TYPE, state)
    }

    @Test
    void detectQualifiedOrNestedTypeInstanceOf() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof A.B.C
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "A"))
        state = state.nextToken(new CloverToken(DOT, "."))
        state = state.nextToken(new CloverToken(IDENT, "B"))
        state = state.nextToken(new CloverToken(DOT, "."))
        state = state.nextToken(new CloverToken(IDENT, "C"))

        assertEquals(InstanceOfState.FULL_TYPE, state)
    }

    @Test
    void detectInstanceOfWithNestedTypeAndPatternMatching() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof A.B[] arr
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "A"))
        state = state.nextToken(new CloverToken(DOT, "."))
        state = state.nextToken(new CloverToken(IDENT, "B"))
        state = state.nextToken(new CloverToken(LBRACK, "["))
        state = state.nextToken(new CloverToken(RBRACK, "]"))
        state = state.nextToken(new CloverToken(IDENT, "arr"))

        assertEquals(InstanceOfState.VARIABLE, state)
    }

    @Test
    void detectPrimitiveArrayTypeWithPatternMatching() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof int[] arr
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(INT, "int"))
        state = state.nextToken(new CloverToken(LBRACK, "["))
        state = state.nextToken(new CloverToken(RBRACK, "]"))
        state = state.nextToken(new CloverToken(IDENT, "arr"))

        assertEquals(InstanceOfState.VARIABLE, state)
    }

    @Test
    void detectInstanceOfWithPatternMatching() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof String str
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "String"))
        state = state.nextToken(new CloverToken(IDENT, "str"))

        assertEquals(InstanceOfState.VARIABLE, state)
    }

    @Test
    void detectInstanceOfWithRecordPattern() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof Point(int x, int y) - a record deconstruction pattern (JEP 440);
        // the '(' after the type must switch to RECORD_DECONSTRUCTION so it is not branch-instrumented
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "Point"))
        state = state.nextToken(new CloverToken(LPAREN, "("))
        state = state.nextToken(new CloverToken(INT, "int"))
        state = state.nextToken(new CloverToken(IDENT, "x"))
        state = state.nextToken(new CloverToken(COMMA, ","))
        state = state.nextToken(new CloverToken(INT, "int"))
        state = state.nextToken(new CloverToken(IDENT, "y"))
        state = state.nextToken(new CloverToken(RPAREN, ")"))

        assertEquals(InstanceOfState.RECORD_DECONSTRUCTION, state)
    }

    @Test
    void detectInstanceOfWithNestedRecordPattern() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof Line(Point(int x, int y), Point p) - a nested record deconstruction pattern;
        // the first '(' after the outer type switches to RECORD_DECONSTRUCTION and it must stay there
        // through the nested pattern so no component (nested or not) is branch-instrumented
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "Line"))
        state = state.nextToken(new CloverToken(LPAREN, "("))
        state = state.nextToken(new CloverToken(IDENT, "Point"))
        state = state.nextToken(new CloverToken(LPAREN, "("))
        state = state.nextToken(new CloverToken(INT, "int"))
        state = state.nextToken(new CloverToken(IDENT, "x"))
        state = state.nextToken(new CloverToken(COMMA, ","))
        state = state.nextToken(new CloverToken(INT, "int"))
        state = state.nextToken(new CloverToken(IDENT, "y"))
        state = state.nextToken(new CloverToken(RPAREN, ")"))
        state = state.nextToken(new CloverToken(COMMA, ","))
        state = state.nextToken(new CloverToken(IDENT, "Point"))
        state = state.nextToken(new CloverToken(IDENT, "p"))
        state = state.nextToken(new CloverToken(RPAREN, ")"))

        assertEquals(InstanceOfState.RECORD_DECONSTRUCTION, state)
    }
}