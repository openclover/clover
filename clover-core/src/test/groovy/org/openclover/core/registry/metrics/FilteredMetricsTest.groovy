package org.openclover.core.registry.metrics

import org.junit.Before
import org.junit.Test
import org.openclover.core.api.registry.BlockMetrics
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.ContextSet
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.api.registry.StatementInfo
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.context.ContextStore

import static org.junit.Assert.assertEquals
import static org.openclover.core.registry.metrics.MetricsHelper.assertMetricsEquals

class FilteredMetricsTest {
    private HasMetricsTestFixture fixture

    private StatementInfo stmtInfo
    private ClassInfo classInfo
    private MethodInfo methodInfo

    /**
     * Sets up a FullProjectInfo model object containing the following:
     * 1 PackageInfo - "testpkg"
     * 1 FileInfo - "testpkg.Test.java"
     * 1 ClassInfo - "testpkg.Test"
     * 1 MethodInfo - "testpkg.Test#method1"
     * 1 StatementInfo - "testpkg.Test#method1 { cmp = 1, startline = 3, hitCount = 0, context = "assert"; }"
     * 1 BranchInfo - "testpkg.Test#method1    { cmp = 2, startline = 4, hitCount = 1, context = "if"; }"
     * @throws IOException
     */
    @Before
    void setUp() throws IOException {
        fixture = new HasMetricsTestFixture(this.getClass().getName())
        //class on line1
        classInfo = fixture.newClass("Test", 1)
        // method on line2 => 1 statement, 1 double branch => complexity = 4
        methodInfo = fixture.newMethod(classInfo, "method1", 2)
        // statement on line3, cmp = 1, hitcoutnt = 0
        stmtInfo = fixture.addStatement(methodInfo, 1, 3, 0)
        stmtInfo.setContext(new ContextSetImpl().set(ContextStore.CONTEXT_ASSERT))
        // branch on line4,
        fixture.addBranch(methodInfo, new ContextSetImpl().set(ContextStore.CONTEXT_IF), 4, 1)
    }

    @Test
    void testEmptyFilter() throws IOException {
        ProjectInfo projectInfo = fixture.getProject()
        projectInfo.setContextFilter(new ContextSetImpl())
        BlockMetrics metrics = projectInfo.getRawMetrics()
        BlockMetrics filteredMetrics = projectInfo.getMetrics()
        assertMetricsEquals(metrics, filteredMetrics)
    }

    /**
     * Test a single filter on a model that has a single statement, and a single branch.
     * @throws IOException
     */
    @Test
    void testSingleStatementFilter() throws IOException {
        ProjectInfo projectInfo = fixture.getProject()
        ContextSet filter = new ContextStore().createContextSetFilter("assert")
        projectInfo.setContextFilter(filter)

        ProjectMetrics filteredMetrics = (ProjectMetrics) projectInfo.getMetrics()
        assertEquals(3, filteredMetrics.getNumElements())
        assertEquals(2, filteredMetrics.getNumBranches())
        assertEquals(0, filteredMetrics.getNumStatements())
        assertEquals(1, filteredMetrics.getNumMethods())
        assertEquals(1, filteredMetrics.getNumClasses())
        assertEquals(1, filteredMetrics.getNumFiles())
        assertEquals(1, filteredMetrics.getNumPackages())

        assertEquals(2, filteredMetrics.getNumCoveredBranches())
        assertEquals(0, filteredMetrics.getNumCoveredStatements())
        assertEquals(0, filteredMetrics.getNumCoveredMethods())
        assertEquals(2, filteredMetrics.getNumCoveredElements())
    }

    @Test
    void testSingleBranchFilter() {
        // now set a brand new context filter for IFs and re-assert
        ContextSet ifContext = new ContextStore().createContextSetFilter("if")
        ProjectInfo projectInfo = fixture.getProject()
        projectInfo.setContextFilter(ifContext)
        ProjectMetrics filteredMetrics = (ProjectMetrics) projectInfo.getMetrics()

        assertEquals(2, filteredMetrics.getNumElements())
        assertEquals(0, filteredMetrics.getNumBranches())
        assertEquals(1, filteredMetrics.getNumStatements())
        assertEquals(1, filteredMetrics.getNumMethods())
        assertEquals(1, filteredMetrics.getNumClasses())
        assertEquals(1, filteredMetrics.getNumFiles())
        assertEquals(1, filteredMetrics.getNumPackages())

        assertEquals(0, filteredMetrics.getNumCoveredBranches())
        assertEquals(0, filteredMetrics.getNumCoveredStatements())
        assertEquals(0, filteredMetrics.getNumCoveredMethods())
        assertEquals(0, filteredMetrics.getNumCoveredElements())
    }

    @Test
    void testFilterIfAndAssertElements() {
        // now set a brand new context filter for IFs and re-assert
        ContextSet ifAssertContext = new ContextStore().createContextSetFilter("if, assert")
        ProjectInfo projectInfo = fixture.getProject()
        projectInfo.setContextFilter(ifAssertContext)
        ProjectMetrics filteredMetrics = (ProjectMetrics) projectInfo.getMetrics()

        assertEquals(1, filteredMetrics.getNumElements())
        assertEquals(0, filteredMetrics.getNumBranches())
        assertEquals(0, filteredMetrics.getNumStatements())
        assertEquals(1, filteredMetrics.getNumMethods())
        assertEquals(1, filteredMetrics.getNumClasses())
        assertEquals(1, filteredMetrics.getNumFiles())
        assertEquals(1, filteredMetrics.getNumPackages())

        assertEquals(0, filteredMetrics.getNumCoveredBranches())
        assertEquals(0, filteredMetrics.getNumCoveredStatements())
        assertEquals(0, filteredMetrics.getNumCoveredMethods())
        assertEquals(0, filteredMetrics.getNumCoveredElements())
    }

}
