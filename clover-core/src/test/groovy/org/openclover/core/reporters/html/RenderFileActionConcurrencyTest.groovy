package org.openclover.core.reporters.html

import junit.framework.TestCase
import org.junit.Ignore
import org.openclover.core.CloverDatabase
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.metrics.HasMetricsTestFixture
import org.openclover.core.reporters.Current
import org.openclover.runtime.api.CloverException

import static org.openclover.core.util.Maps.newHashMap

/**
 * Reproduces OC issue #122: RenderFileAction holds its column/context state in *static*
 * ThreadLocal fields whose lifecycle is owned by HtmlReporter. Any second report finishing
 * in the same JVM nulls those statics out from under a report that is still rendering,
 * producing an NPE in RenderFileAction.call().
 */
@Ignore
class RenderFileActionConcurrencyTest extends TestCase {

    HasMetricsTestFixture fixture
    Current config
    CloverDatabase cloverDatabase

    void setUp() throws IOException, CloverException {
        fixture = new HasMetricsTestFixture("Render File Action Concurrency Test")

        Clover2Registry reg = fixture.createSampleRegistry()
        fixture.setProject(reg.getProject())
        config = HtmlReporter.processArgs([
                "-i", fixture.getInitStr(), "-o", fixture.getTmpDir().getAbsolutePath()
        ] as String[])

        cloverDatabase = new CloverDatabase(fixture.getInitStr())
        cloverDatabase.loadCoverageData()
    }

    void tearDown() {
        RenderFileAction.resetThreadLocals()
    }

    private RenderFileAction newAction() {
        PackageInfo pinfo = fixture.newPackage("com.clover.test")
        FullFileInfo finfo = fixture.newFile(pinfo, "TestFileInfo.java")
        return new RenderFileAction(finfo, new HtmlRenderingSupportImpl(), config,
                VelocityContextBuilder.create(), cloverDatabase, fixture.getProject(),
                newHashMap())
    }

    /**
     * A report that is still rendering when another report's finally-block runs
     * resetThreadLocals() dies with an NPE at RenderFileAction.call().
     */
    void testResetByAnotherReportCausesNPE() {
        RenderFileAction.initThreadLocals()   // report A starts
        RenderFileAction action = newAction()

        RenderFileAction.resetThreadLocals()  // report B's finally-block wins the race

        try {
            action.call()                     // report A's still-queued task
            fail("Expected NPE from RenderFileAction.call()")
        } catch (NullPointerException expected) {
            // this is the stack frame users report in issue #122
        }
    }

    /**
     * The same statics are shared by every concurrent report, so a task submitted by one
     * report observes columns configured by another.
     */
    void testThreadLocalsAreStaticAndShared() {
        RenderFileAction.initThreadLocals()
        def first = RenderFileAction.columnsTL

        RenderFileAction.initThreadLocals()   // a second report re-inits the same statics
        assertNotSame("statics are shared across reports", first, RenderFileAction.columnsTL)
    }
}
