package org.openclover.core.registry.entities

import junit.framework.TestCase
import org.openclover.core.api.registry.ContextSet
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.context.ContextStore
import org.openclover.core.context.StatementRegexpContext
import org.openclover.core.registry.metrics.HasMetricsTestFixture
import org.openclover.runtime.api.CloverException

import java.util.regex.Pattern

class FullElementInfoTest extends TestCase {


    void testIsFilteredBuiltIns() throws Exception {

        FullStatementInfo stmtInfo = createStmtInfo()
        stmtInfo.setContext(stmtInfo.getContext().set(ContextStore.CONTEXT_IF))

        ContextSet mask = new ContextStore().createContextSetFilter("private, static", false)
        assertFalse(stmtInfo.isFiltered(mask))
        mask = new ContextStore().createContextSetFilter("if, private, static", false)
        assertTrue(stmtInfo.isFiltered(mask))

        assertFalse(stmtInfo.isFiltered(null))

    }

    void testIsFilteredWithSingleCustomFilter() throws IOException, CloverException {
        FullStatementInfo stmtInfo = createStmtInfo()
        ContextStore ctxReg = new ContextStore()
        final int filterIdx = addStatementContext(ctxReg, "filter", '^*filter*$')

        stmtInfo.setContext(stmtInfo.getContext().set(filterIdx)); // set the current context to "filter"
        ContextSet mask = ctxReg.createContextSetFilter("filter", false)
        assertTrue(stmtInfo.isFiltered(mask))

        mask = ctxReg.createContextSetFilter("static", false)
        assertFalse(stmtInfo.isFiltered(mask))
    }

    void testIsFilteredWithCustomFilters() throws IOException, CloverException {

        FullStatementInfo stmtInfo = createStmtInfo()
        ContextStore ctxReg = new ContextStore()

        final int filterIdx = addStatementContext(ctxReg, "filter", '^*filter*$')
        final int ignoreIdx = addStatementContext(ctxReg, "ignore", '^*ignore*$')
        final int testIdx =   addStatementContext(ctxReg, "test",   '^*test*$')

        // all contexts
        stmtInfo.setContext(
            stmtInfo.getContext()
                .set(filterIdx)
                .set(ignoreIdx)
                .set(testIdx))

        ContextSet mask = ctxReg.createContextSetFilter("test, filter, ignore", false)
        assertTrue(stmtInfo.isFiltered(mask))

        // single context
        stmtInfo = createStmtInfo()
        stmtInfo.setContext(stmtInfo.getContext().set(filterIdx))
        assertTrue(stmtInfo.isFiltered(mask))

        // no context
        stmtInfo = createStmtInfo()
        assertFalse(stmtInfo.isFiltered(mask))

    }

    private int addStatementContext(ContextStore ctxReg, String name, String regExp) throws CloverException {
        return ctxReg.addStatementContext(new StatementRegexpContext(name, Pattern.compile(regExp)))
    }

    FullStatementInfo createStmtInfo() throws IOException {
        HasMetricsTestFixture fixture = new HasMetricsTestFixture("testIsFiltered")
        FullClassInfo classInfo = fixture.newClass("TestIsFiltered", 2)
        FullMethodInfo methInfo = fixture.newMethod(classInfo, "testIt", 3)
        FullStatementInfo stmInfo = fixture.addStatement(methInfo, 3, 4, 10)
        stmInfo.setContext(new ContextSetImpl())

        return stmInfo
    }

}
