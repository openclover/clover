package org.openclover.idea.coverage;

import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.util.ui.UIUtil;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.registry.metrics.HasMetricsFilter;
import org.openclover.core.reporters.filters.DefaultTestFilter;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.util.ModelScope;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DefaultCoverageManagerTest extends LightIdeaTestCase {
    private IdeaCloverConfig config;
    private DefaultCoverageManager dcm;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        config = ProjectPlugin.getPlugin(getProject()).getConfig();
        config.setLoadPerTestData(true);
        dcm = new DefaultCoverageManager(getProject(),
                getClass().getResource("/clover/coverage.db").getPath(),
                HasMetricsFilter.ACCEPT_ALL,
                new DefaultTestFilter());
    }

    public void testReloadingCoverageWithPassedTestsOnly() throws InterruptedException {

        config.setIncludePassedTestCoverageOnly(false);
        loadDCM(dcm);
        final float full1 = testModelAndTreeCoherence(dcm.getCoverageTree());

        config.setIncludePassedTestCoverageOnly(true);
        loadDCM(dcm);
        final float full2 = testModelAndTreeCoherence(dcm.getCoverageTree());

        assertTrue(full1 > full2);

    }

    private float testModelAndTreeCoherence(CoverageTreeModel coverageTreeModel2) {
        final CloverDatabase db = coverageTreeModel2.getCloverDatabase();
        final float app = db.getAppOnlyModel().getMetrics().getPcCoveredStatements();
        final float test = db.getTestOnlyModel().getMetrics().getPcCoveredStatements();
        final float full = db.getFullModel().getMetrics().getPcCoveredStatements();
        final float nodeFull = retrieveFirstPackage(coverageTreeModel2, ModelScope.ALL_CLASSES).getPcCoveredStatements();
        final float nodeApp = retrieveFirstPackage(coverageTreeModel2, ModelScope.APP_CLASSES_ONLY).getPcCoveredStatements();
        final float nodeTest = retrieveFirstPackage(coverageTreeModel2, ModelScope.TEST_CLASSES_ONLY).getPcCoveredStatements();

        assertEquals("Comparing APP_CLASSES model and tree", app, nodeApp);
        assertEquals("Comparing TEST_CLASSES model and tree", test, nodeTest);
        assertEquals("Comparing ALL_CLASSES model and tree", full, nodeFull);

        return full;
    }

    private static BlockMetrics retrieveFirstPackage(CoverageTreeModel model, ModelScope scope) {
        return ((CoverageTreeModel.NodeWrapper)((DefaultMutableTreeNode)model.getClassTree(true, scope).getChildAt(0)).getUserObject()).getHasMetrics().getMetrics();
    }

    private static void loadDCM(final DefaultCoverageManager dcm) throws InterruptedException {
        final CountDownLatch loadLatch = new CountDownLatch(1);
        dcm.reload(newModel -> {
            dcm.setCoverageTree(newModel);
            loadLatch.countDown();
        });
        UIUtil.dispatchAllInvocationEvents(); // force to call onSuccess() post-task action

        assertTrue("Waiting 10 seconds for coverage load", loadLatch.await(10, TimeUnit.SECONDS));
    }
}
