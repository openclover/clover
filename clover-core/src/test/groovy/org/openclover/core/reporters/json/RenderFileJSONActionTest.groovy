package org.openclover.core.reporters.json

import clover.org.apache.velocity.VelocityContext
import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.reporters.json.JSONObject
import com.atlassian.clover.reporters.json.JSONReporter
import com.atlassian.clover.reporters.json.RenderFileJSONAction
import com.atlassian.clover.reporters.json.RenderMetricsJSONAction
import org.openclover.runtime.api.CloverException
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.metrics.PackageMetrics
import com.atlassian.clover.reporters.Current
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl
import com.atlassian.clover.util.CloverUtils
import junit.framework.TestCase

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
