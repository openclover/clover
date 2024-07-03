package org.openclover.core.reporters.html

import org.apache.velocity.VelocityContext
import junit.framework.TestCase
import org.openclover.core.CloverDatabase
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.metrics.HasMetricsTestFixture
import org.openclover.core.reporters.Current
import org.openclover.core.spi.reporters.html.source.LineRenderInfo
import org.openclover.runtime.api.CloverException

import static org.openclover.core.util.Maps.newHashMap

class RenderFileActionTest extends TestCase {

    HasMetricsTestFixture fixture
    Current config
    CloverDatabase cloverDatabase

    void setUp() throws IOException, CloverException {
        RenderFileAction.initThreadLocals()
        fixture = new HasMetricsTestFixture("Render File Action Test")

        Clover2Registry reg = fixture.createSampleRegistry()
        fixture.setProject(reg.getProject())
        config = HtmlReporter.processArgs([
                "-i", fixture.getInitStr(), "-o", fixture.getTmpDir().getAbsolutePath()
        ] as String[])

        cloverDatabase = new CloverDatabase(fixture.getInitStr())
        cloverDatabase.loadCoverageData();        

        assertNotNull("Invalid configuration.", config)
        assertTrue("Invalid configuration.", config.validate())

    }

    void tearDown() {
        RenderFileAction.resetThreadLocals()
    }

    void testInsertSrcFileProperties() throws Exception {
        HtmlRenderingSupportImpl helper = new HtmlRenderingSupportImpl()
        VelocityContext ctx = new VelocityContext()

        PackageInfo pinfo = fixture.newPackage("com.clover.test")
        FullFileInfo finfo = fixture.newFile(pinfo, "TestFileInfo.java")

        RenderFileAction action = new RenderFileAction(finfo, helper, config, ctx,
                                                       cloverDatabase, fixture.getProject(),
                                                       newHashMap())
        action.call()

        assertEquals(ctx.get("headerMetrics"), finfo.getMetrics())
        assertEquals(ctx.get("headerMetricsRaw"), finfo.getRawMetrics())
        assertEquals(ctx.get("fileInfo"), finfo)
        assertEquals(ctx.get("projInfo"), fixture.getProject())
        assertEquals(ctx.get("cloverDb"), cloverDatabase)

        assertTrue(ctx.get("testMetrics") instanceof Map)
        assertTrue(ctx.get("numTargetMethods") instanceof Integer)
        assertTrue(ctx.get("testsPerFile") instanceof Integer)

        assertTrue(ctx.get("renderInfo") instanceof LineRenderInfo[])
    }

}
