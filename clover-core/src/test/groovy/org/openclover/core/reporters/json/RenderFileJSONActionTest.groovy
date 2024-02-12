package org.openclover.core.reporters.json

import clover.org.apache.velocity.VelocityContext
import junit.framework.TestCase
import org.openclover.core.CloverDatabase
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullPackageInfo
import org.openclover.core.registry.metrics.HasMetricsTestFixture
import org.openclover.core.registry.metrics.PackageMetrics
import org.openclover.core.reporters.Current
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl
import org.openclover.core.util.CloverUtils
import org.openclover.runtime.api.CloverException

class RenderFileJSONActionTest extends TestCase {
    private HasMetricsTestFixture fixture
    private Current config
    private CloverDatabase cloverDatabase

    void setUp() throws IOException, CloverException {
        RenderFileJSONAction.initThreadLocals()

        fixture = new HasMetricsTestFixture("Render JSON Action Test")

        Clover2Registry reg = fixture.createSampleRegistry()
        fixture.setProject(reg.getProject())
        config = JSONReporter.processArgs([
                "-i", fixture.getInitStr(), "-o", fixture.getTmpDir().getAbsolutePath()
        ] as String[])

        cloverDatabase = new CloverDatabase(fixture.getInitStr())
        cloverDatabase.loadCoverageData()

        assertNotNull("Invalid configuration.", config)
        assertTrue("Invalid configuration.", config.validate())

    }

    void tearDown() {
        RenderFileJSONAction.resetThreadLocals()
    }

    void testRenderJSON() throws Exception {
        HtmlRenderingSupportImpl helper = new HtmlRenderingSupportImpl()

        VelocityContext ctx = new VelocityContext()

        FullPackageInfo pinfo = fixture.newPackage("com.clover.test")
        FullFileInfo finfo = fixture.newFile(pinfo, "TestFileInfo.java")

        RenderFileJSONAction action =
            new RenderFileJSONAction(
                finfo, helper, config, ctx, cloverDatabase, fixture.getProject())
        action.call()

        final String jsonString = (String) ctx.get("json")
        new JSONObject(jsonString)
        
        assertTrue(jsonString instanceof String);   
    }

    void testRenderMetricsJSON() throws Exception {
        HtmlRenderingSupportImpl helper = new HtmlRenderingSupportImpl()

        VelocityContext ctx = new VelocityContext()

        FullPackageInfo pinfo = fixture.newPackage("com.clover.test")
        fixture.newFile(pinfo, "TestFileInfo.java")
        PackageMetrics pkgMetrics = (PackageMetrics) pinfo.getMetrics()
        pkgMetrics.setComplexity(56)
        pkgMetrics.setNumCoveredMethods(10)
        pkgMetrics.setNumMethods(15)


        File outfile = new File(CloverUtils.createOutDir(pinfo, config.getOutFile()), "package.js")

        RenderMetricsJSONAction.initThreadLocals()
        RenderMetricsJSONAction action =
                new RenderMetricsJSONAction(ctx, pinfo, config, outfile, helper)
        action.call()
        RenderMetricsJSONAction.resetThreadLocals()
        
        final JSONObject json = (JSONObject) ctx.get("json")
        JSONObject stats = json.getJSONObject("stats")
        assertEquals(pkgMetrics.getNumElements(), stats.getInt("TotalElements"))
        assertEquals(pkgMetrics.getLineCount(), stats.getInt("LineCount"))
        assertEquals(pkgMetrics.getNcLineCount(), stats.getInt("NcLineCount"))
        assertEquals(pkgMetrics.getComplexity(), stats.getInt("Complexity"))
        assertEquals("TotalPercentageCovered incorrect.",
                (pkgMetrics.getPcCoveredElements() * 100f) as float,
                stats.getDouble("TotalPercentageCovered") as float, 0f)
    }
}
