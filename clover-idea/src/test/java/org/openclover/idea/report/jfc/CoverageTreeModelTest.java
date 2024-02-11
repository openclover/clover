package org.openclover.idea.report.jfc;

import org.openclover.idea.util.ModelScope;
import org.openclover.core.ProgressListener;
import org.openclover.idea.coverage.CoverageTreeModel;
import com.intellij.testFramework.LightIdeaTestCase;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * CoverageTreeModel Tester.
 */
public class CoverageTreeModelTest extends LightIdeaTestCase {
    private static final int TESTS_PASSING = 10;
    private static final int TESTS_FAILING = 1;
    private static final int TESTS_RUN = TESTS_PASSING + TESTS_FAILING;

    public void testRecordedTestCases() {
        final String path = getClass().getResource("/clover/coverage.db").getPath();
        CoverageTreeModel model = new CoverageTreeModel("Test root name", path, 0, "", true, null, ModelScope.ALL_CLASSES, /*load per test data*/ true, null, null, null);
        model.load(ProgressListener.NOOP_LISTENER);

        DefaultMutableTreeNode rootNode = model.getClassTree(true, ModelScope.ALL_CLASSES);
        CoverageTreeModel.NodeWrapper rootObject = (CoverageTreeModel.NodeWrapper) rootNode.getUserObject();
        assertEquals(TESTS_RUN, rootObject.getTestPassInfo().getTestsRun());
        assertEquals(TESTS_PASSING, rootObject.getTestPassInfo().getTestPasses());
        assertEquals(TESTS_FAILING, rootObject.getTestPassInfo().getTestFailures());

        CoverageTreeModel.NodeWrapper pkgObject = (CoverageTreeModel.NodeWrapper) ((DefaultMutableTreeNode) rootNode.getFirstChild()).getUserObject();
        assertEquals("com", pkgObject.getName());
        assertEquals(TESTS_RUN, pkgObject.getTestPassInfo().getTestsRun());
        assertEquals(TESTS_PASSING, pkgObject.getTestPassInfo().getTestPasses());
        assertEquals(TESTS_FAILING, pkgObject.getTestPassInfo().getTestFailures());

        rootNode = model.getClassTree(true, ModelScope.APP_CLASSES_ONLY);
        rootObject = (CoverageTreeModel.NodeWrapper) rootNode.getUserObject();
        assertEquals(TESTS_RUN, rootObject.getTestPassInfo().getTestsRun());
        assertEquals(TESTS_PASSING, rootObject.getTestPassInfo().getTestPasses());
        assertEquals(TESTS_FAILING, rootObject.getTestPassInfo().getTestFailures());

        pkgObject = (CoverageTreeModel.NodeWrapper) ((DefaultMutableTreeNode) rootNode.getFirstChild()).getUserObject();
        assertEquals("com", pkgObject.getName());
        assertEquals(TESTS_RUN, pkgObject.getTestPassInfo().getTestsRun());
        assertEquals(TESTS_PASSING, pkgObject.getTestPassInfo().getTestPasses());
        assertEquals(TESTS_FAILING, pkgObject.getTestPassInfo().getTestFailures());

        rootNode = model.getClassTree(false, ModelScope.APP_CLASSES_ONLY);
        rootObject = (CoverageTreeModel.NodeWrapper) rootNode.getUserObject();
        assertEquals(TESTS_RUN, rootObject.getTestPassInfo().getTestsRun());
        assertEquals(TESTS_PASSING, rootObject.getTestPassInfo().getTestPasses());
        assertEquals(TESTS_FAILING, rootObject.getTestPassInfo().getTestFailures());

        pkgObject = (CoverageTreeModel.NodeWrapper) ((DefaultMutableTreeNode) rootNode.getFirstChild()).getUserObject();
        assertEquals("com.cenqua.clovertest", pkgObject.getName());
        assertEquals(TESTS_RUN, pkgObject.getTestPassInfo().getTestsRun());
        assertEquals(TESTS_PASSING, pkgObject.getTestPassInfo().getTestPasses());
        assertEquals(TESTS_FAILING, pkgObject.getTestPassInfo().getTestFailures());

        CoverageTreeModel.NodeWrapper clsObject = (CoverageTreeModel.NodeWrapper) ((DefaultMutableTreeNode) rootNode.getFirstChild().getChildAt(0)).getUserObject();
        assertEquals("A", clsObject.getName());
        assertEquals(7, clsObject.getTestPassInfo().getTestsRun());
        assertEquals(7, clsObject.getTestPassInfo().getTestPasses());
        assertEquals(0, clsObject.getTestPassInfo().getTestFailures());

        CoverageTreeModel.NodeWrapper methodObject = (CoverageTreeModel.NodeWrapper) ((DefaultMutableTreeNode) rootNode.getFirstChild().getChildAt(0).getChildAt(0)).getUserObject();
        assertEquals("a1() : void", methodObject.getName());
        assertEquals(3, methodObject.getTestPassInfo().getTestsRun());
        assertEquals(3, methodObject.getTestPassInfo().getTestPasses());
        assertEquals(0, methodObject.getTestPassInfo().getTestFailures());


    }

    public void testNoPerTestDataCoverage() {
        final String path = getClass().getResource("/clover/coverage.db").getPath();
        final CoverageTreeModel model = new CoverageTreeModel("Test root name", path, 0, "", true, null, ModelScope.ALL_CLASSES, /*don't load per test data*/ false, null, null, null);
        model.load(ProgressListener.NOOP_LISTENER);
        assertFalse(model.getCloverDatabase().getFullModel().hasTestResults());

    }
}
