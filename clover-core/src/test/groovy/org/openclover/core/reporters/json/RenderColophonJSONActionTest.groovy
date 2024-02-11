package org.openclover.core.reporters.json

import org.openclover.core.reporters.Current
import org.openclover.core.reporters.Format
import org.openclover.core.util.FileUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class RenderColophonJSONActionTest {
    Current cfg
    File outDir

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() throws IOException {
        outDir = FileUtils.createTempDir(testName.methodName)
        cfg = new Current()
        cfg.setFormat(Format.DEFAULT_JSON)
    }

    @Test
    void testRender() throws Exception {
        // final long start = System.currentTimeMillis()
        // 
        // final File colophon = new File(outDir, "colophon.js")
        // final VelocityContext ctx = new VelocityContext()
        // new RenderColophonJSONAction(ctx, colophon, cfg).call()
        // 
        // final JSONObject json = (JSONObject)ctx.get("json")
        // assertNotNull(json)
        // 
        // final JSONObject clover = json.getJSONObject("clover")
        // assertNotNull(clover)
        // assertEquals(CloverVersionInfo.RELEASE_NUM, clover.getString("release"))
        // 
        // final JSONObject cloverBuild = clover.getJSONObject("build")
        // assertNotNull(cloverBuild)
        // assertEquals(CloverVersionInfo.BUILD_STAMP, cloverBuild.getLong("stamp"))
        // assertEquals(CloverVersionInfo.BUILD_DATE, cloverBuild.getString("date"))
        //
        // final JSONObject report = json.getJSONObject("report")
        // assertNotNull(report)
        // assertEquals(cfg.getFormat().getCallback(), report.getString("callback"))
        // final long reportStamp = report.getLong("stamp")
        // assertTrue(start <= reportStamp)
        // assertTrue(System.currentTimeMillis() >= reportStamp)
    }
}
