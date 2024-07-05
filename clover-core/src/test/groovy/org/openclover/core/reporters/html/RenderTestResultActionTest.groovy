package org.openclover.core.reporters.html

import junit.framework.TestCase
import org.openclover.core.CloverDatabase
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullTestCaseInfo
import org.openclover.core.registry.metrics.HasMetricsTestFixture
import org.openclover.core.reporters.Current
import org.openclover.runtime.api.CloverException

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

        VelocityContextBuilder ctx = VelocityContextBuilder.create()

        PackageInfo pinfo = fixture.newPackage("com.clover.test")
        FullFileInfo finfo = fixture.newFile(pinfo, "TestFileInfo.java")
        FullClassInfo classInfo = fixture.newClass(finfo, "TestClass", 2)
        FullMethodInfo methodInfo = fixture.newMethod(classInfo, "testing", 3)
        methodInfo.setStaticTestName("testing-static")
        FullTestCaseInfo test = new FullTestCaseInfo(new Integer(1), classInfo, methodInfo, "testing-runtime")

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

        VelocityContextBuilder ctx = VelocityContextBuilder.create()

        PackageInfo pinfo = fixture.newPackage("com.clover.test")
        FullFileInfo finfo = fixture.newFile(pinfo, "TestFileInfo.java")
        FullClassInfo classInfo = fixture.newClass(finfo, "TestClass", 2)
        FullMethodInfo methodInfo = fixture.newMethod(classInfo, "testing", 3)
        methodInfo.setStaticTestName("testing-static")
        FullTestCaseInfo test = new FullTestCaseInfo(new Integer(1), classInfo, methodInfo, "testing-runtime")

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
