package org.openclover.core.instr.java

import org.junit.Test

import static org.junit.Assert.assertTrue

public class EmitterTest {

    @Test
    void testEmitterDependency() throws Exception {
        // empty context
        String input = "testing"
        String depend = "depend"
        SimpleEmitter se = new SimpleEmitter(input)
        StringWriter out = new StringWriter()
        se.emit(out)
        assertTrue(out.toString().equals(input))
        se.setEnabled(false)
        se.emit(out)
        assertTrue(out.toString().equals(input))

        // check dependencies
        SimpleEmitter dep = new SimpleEmitter(depend)
        se.addDependent(dep)
        se.emit(out)
        dep.emit(out)
        assertTrue(out.toString().equals(input))
        se.setEnabled(true)
        se.emit(out)
        dep.emit(out)
        assertTrue(out.toString().equals(input+input+depend))
    }

}
