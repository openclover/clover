package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.api.registry.ContextSet
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.context.ContextStore

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertTrue

class ContextTreeNodeTest {
    @Test
    void testEnterExit() {
        ContextTreeNode root = new ContextTreeNode(ContextStore.NEXT_INDEX, new ContextSetImpl())
        root = root.enterContext(ContextStore.CONTEXT_METHOD)
        ContextSet methodContext = root.getContext()
        root = root.enterContext(ContextStore.CONTEXT_IF)
        ContextSet ifContext = root.getContext()
        root = root.exitContext()
        ContextSet methodContext2 = root.getContext()
        root = root.exitContext()

        assertTrue(root.getContext().equals(new ContextSetImpl()))
        assertSame(methodContext, methodContext2)
        assertTrue(methodContext.get(ContextStore.CONTEXT_METHOD))
        assertFalse(methodContext.get(ContextStore.CONTEXT_IF))
        assertTrue(ifContext.get(ContextStore.CONTEXT_METHOD))
        assertTrue(ifContext.get(ContextStore.CONTEXT_IF))
    }
}
