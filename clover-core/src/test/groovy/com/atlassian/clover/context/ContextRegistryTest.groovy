package com.atlassian.clover.context

import com.atlassian.clover.api.CloverException
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

class ContextRegistryTest {

    @Test
    void testRegistryAdditions() throws Exception {
        ContextStore ctxreg = new ContextStore()

        // normal add
        int index1 = ctxreg.addMethodContext(new MethodRegexpContext("foo", Pattern.compile(".*")))

        // add another with same name
        int index2 = ctxreg.addMethodContext(new MethodRegexpContext("foo", Pattern.compile(".*")))

        // add another with same name, different type
        int index3 = ctxreg.addStatementContext(new StatementRegexpContext("foo", Pattern.compile(".*")))

        assertEquals("Should resuse index if ctx of same name already exists", index1, index2)
        assertEquals("Should resuse index if ctx of same name already exists", index2, index3)

        // reserved name add
        try {
           ctxreg.addMethodContext(new MethodRegexpContext("method", Pattern.compile(".*")))
           fail("Context with reserved name accepted")
        } catch (CloverException e) {

        }
        // reserved name add
        try {
           ctxreg.addStatementContext(new StatementRegexpContext("method", Pattern.compile(".*")))
           fail("Context with reserved name accepted")
        } catch (CloverException e) {

        }
    }

    @Test
    void testGetMaskFromFilterSpec() {
        ContextStore ctxReg = new ContextStore()

        // test with a single filter
        ContextSet staticSet = createSetFor(ContextStore.CONTEXT_STATIC)
        ContextSet set = ctxReg.createContextSetFilter("static")
        assertEquals(staticSet.toString(), set.toString())

        // test an empty filter default
        final ContextSet defaultSet = createSetFor(ContextStore.CONTEXT_CLOVER_OFF)
        set = ctxReg.createContextSetFilter("")
        assertEquals(set, defaultSet)

        // test three filters 
        ContextSet multiSet =
            createSetFor(ContextStore.CONTEXT_ASSERT)
                .set(ContextStore.CONTEXT_CATCH)
                .set(ContextStore.CONTEXT_CTOR)
        String spec = "assert, catch, constructor"
        set = ctxReg.createContextSetFilter(spec)
        assertEquals(multiSet.toString(), set.toString())

        // test two unkown filters
        set = ctxReg.createContextSetFilter("nofilter1, nofilter2")
        assertEquals(set, defaultSet)
    }

    @Test
    void testGetContexts() throws Exception {
        ContextStore ctxReg = new ContextStore()
        ContextSet emptySet = new ContextSet()
        // test empty
        NamedContext[] noContexts = ctxReg.getContexts(emptySet)
        assertEquals(0, noContexts.length)

        ContextSet soloSet = new ContextSet()
        soloSet = soloSet.set(ContextStore.CONTEXT_ASSERT)
        NamedContext[] singleContext = ctxReg.getContexts(soloSet)
        // test single
        assertEquals(1, singleContext.length)
        assertEquals("assert", singleContext[0].getName())

        ContextSet multiSet = new ContextSet()
        multiSet = multiSet.set(ContextStore.CONTEXT_ASSERT)
        multiSet = multiSet.set(ContextStore.CONTEXT_CATCH)
        // test >1 entry.
        NamedContext[] manyContexts = ctxReg.getContexts(multiSet)
        assertEquals(2, manyContexts.length)
        assertEquals("catch", manyContexts[0].getName())
        assertEquals("assert", manyContexts[1].getName())
    }

    @Test
    void testGetNamedContextsFor() throws Exception {
        ContextStore registry = new ContextStore()

        ContextSet ctxSet = new ContextSet()
        String contextString = registry.getContextsAsString(ctxSet)
        assertEquals("", contextString)

        ctxSet = ctxSet.set(ContextStore.CONTEXT_ASSERT)
        contextString = registry.getContextsAsString(ctxSet)
        assertEquals("assert", contextString)

        ctxSet = ctxSet.set(ContextStore.CONTEXT_CLOVER_OFF)
        contextString = registry.getContextsAsString(ctxSet)
        assertEquals("SourceDirective, assert", contextString)
        assertEquals("SourceDirective, assert", registry.getContextsAsString(ctxSet)) // tests the cache
    }

    private static ContextSet createSetFor(int contextIndex) {
        new ContextSet()
                .set(contextIndex)
                .set(ContextStore.CONTEXT_CLOVER_OFF)
    }
}
