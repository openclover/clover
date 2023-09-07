package com.atlassian.clover.idea.coverage;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.util.ModelScope;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.util.ui.UIUtil;

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
        dcm.reload(new AcceptsCoverageTreeModel() {
            @Override
            public void setCoverageTree(CoverageTreeModel newModel) {
                dcm.setCoverageTree(newModel);
                loadLatch.countDown();
            }
        });
        UIUtil.dispatchAllInvocationEvents(); // force to call onSuccess() post-task action

        assertTrue("Waiting 10 seconds for coverage load", loadLatch.await(10, TimeUnit.SECONDS));
    }
}
