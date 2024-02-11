package org.openclover.core.instr.java

import org.junit.Test

import static org.openclover.core.instr.java.JavaTokenTypes.DOT
import static org.openclover.core.instr.java.JavaTokenTypes.IDENT
import static org.openclover.core.instr.java.JavaTokenTypes.INSTANCEOF
import static org.openclover.core.instr.java.JavaTokenTypes.LBRACK
import static org.openclover.core.instr.java.JavaTokenTypes.RBRACK
import static org.junit.Assert.assertEquals

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
    void detectInstanceOfWithPatternMatching() {
        InstanceOfState state = InstanceOfState.NOTHING;

        // obj instanceof String str
        state = state.nextToken(new CloverToken(IDENT, "obj"))
        state = state.nextToken(new CloverToken(INSTANCEOF, "instanceof"))
        state = state.nextToken(new CloverToken(IDENT, "String"))
        state = state.nextToken(new CloverToken(IDENT, "str"))

        assertEquals(InstanceOfState.VARIABLE, state)
    }
}