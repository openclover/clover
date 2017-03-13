package com.atlassian.clover.reporters.html

import clover.org.apache.velocity.VelocityContext
import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.api.CloverException
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.metrics.HasMetricsTestFixture
import com.atlassian.clover.registry.entities.TestCaseInfo
import com.atlassian.clover.reporters.Current
import junit.framework.TestCase

class RenderTestResultActionTest extends TestCase {
    HasMetricsTestFixture fixture
    Current config
    CloverDatabase cloverDatabase

    void setUp() throws IOException, CloverException {
        fixture = new HasMetricsTestFixture("Render File Action Test")

        Clover2Registry reg = fixture.createSampleRegistry()
        fixture.setProject(reg.getProject())
        config = HtmlReporter.processArgs([
                "-i", fixture.getInitStr(), "-o", fixture.getTmpDir().getAbsolutePath()
        ] as String[])
        config.setShowUniqueCoverage(false)
        cloverDatabase = new CloverDatabase(fixture.getInitStr())
        cloverDatabase.loadCoverageData();        
        assertNotNull("Invalid configuration.", config)
        assertTrue("Invalid configuration.", config.validate())
    }


    void testRender() throws Exception {
        HtmlRenderingSupportImpl helper = new HtmlRenderingSupportImpl()

        config.setShowUniqueCoverage(true)

        VelocityContext ctx = new VelocityContext()

        FullPackageInfo pinfo = fixture.newPackage("com.clover.test")
        FullFileInfo finfo = fixture.newFile(pinfo, "TestFileInfo.java")
        FullClassInfo classInfo = fixture.newClass(finfo, "TestClass", 2)
        FullMethodInfo methodInfo = fixture.newMethod(classInfo, "testing", 3)
        methodInfo.setStaticTestName("testing-static")
        TestCaseInfo test = new TestCaseInfo(new Integer(1), classInfo, methodInfo, "testing-runtime")

        RenderTestResultAction action = new RenderTestResultAction(test, helper, config, fixture.getProject(), ctx, fixture.getProject(), cloverDatabase)
        action.call()

        assertTrue(ctx.get("currentPageURL") instanceof StringBuffer)
        assertTrue(ctx.get("targetClasses") instanceof List)
        assertTrue(ctx.get("showUnique") instanceof Boolean)
        assertTrue(((Boolean)ctx.get("showUnique")).booleanValue())
        assertTrue(ctx.get("uniqueTargetClasses") instanceof Map)
        assertTrue(ctx.get("pcUniqueCoverage") instanceof String)
        assertEquals(test, ctx.get("test"))
        assertEquals(Boolean.TRUE, ctx.get("topLevel"))
        assertEquals(fixture.getProject(), ctx.get("projectInfo"))
        assertTrue(ctx.get("hasResults") instanceof Boolean)
    }

    void testRenderWithoutUniqueCoverage() throws Exception {
        HtmlRenderingSupportImpl helper = new HtmlRenderingSupportImpl()

        VelocityContext ctx = new VelocityContext()

        FullPackageInfo pinfo = fixture.newPackage("com.clover.test")
        FullFileInfo finfo = fixture.newFile(pinfo, "TestFileInfo.java")
        FullClassInfo classInfo = fixture.newClass(finfo, "TestClass", 2)
        FullMethodInfo methodInfo = fixture.newMethod(classInfo, "testing", 3)
        methodInfo.setStaticTestName("testing-static")
        TestCaseInfo test = new TestCaseInfo(new Integer(1), classInfo, methodInfo, "testing-runtime")

        RenderTestResultAction action = new RenderTestResultAction(test, helper, config, fixture.getProject(), ctx, fixture.getProject(), cloverDatabase)
        action.call()

        assertTrue(ctx.get("currentPageURL") instanceof StringBuffer)
        assertTrue(ctx.get("targetClasses") instanceof List)
        assertTrue(ctx.get("showUnique") instanceof Boolean)
        assertFalse(((Boolean)ctx.get("showUnique")).booleanValue())
        assertNull(ctx.get("uniqueTargetClasses"))
        assertNull(ctx.get("pcUniqueCoverage"))
        assertEquals(test, ctx.get("test"))
        assertEquals(Boolean.TRUE, ctx.get("topLevel"))
        assertEquals(fixture.getProject(), ctx.get("projectInfo"))
        assertTrue(ctx.get("hasResults") instanceof Boolean)
    }
}
