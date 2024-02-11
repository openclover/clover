package org.openclover.core.instr.java

import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.context.ContextStore
import com.atlassian.clover.instr.java.ContextTreeNode
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertTrue

class ContextTreeNodeTest {
    @Test
    void testEnterExit() {
        ContextTreeNode root = new ContextTreeNode(ContextStore.NEXT_INDEX, new ContextSet())
        root = root.enterContext(ContextStore.CONTEXT_METHOD)
        ContextSet methodContext = root.getContext()
        root = root.enterContext(ContextStore.CONTEXT_IF)
        ContextSet ifContext = root.getContext()
        root = root.exitContext()
        ContextSet methodContext2 = root.getContext()
        root = root.exitContext()

        assertTrue(root.getContext().equals(new ContextSet()))
        assertSame(methodContext, methodContext2)
        assertTrue(methodContext.get(ContextStore.CONTEXT_METHOD))
        assertFalse(methodContext.get(ContextStore.CONTEXT_IF))
        assertTrue(ifContext.get(ContextStore.CONTEXT_METHOD))
        assertTrue(ifContext.get(ContextStore.CONTEXT_IF))
    }
}
