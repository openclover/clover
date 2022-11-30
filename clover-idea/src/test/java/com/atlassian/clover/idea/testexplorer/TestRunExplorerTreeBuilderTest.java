package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.idea.config.TestCaseLayout;
import com.atlassian.clover.idea.treetables.SortableListTreeTableModelOnColumns;
import com.atlassian.clover.idea.treetables.TreeTableModelFactory;
import com.atlassian.clover.idea.util.ui.TreeUtil;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import junit.framework.TestCase;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

/**
 * TestRunExplorerToolWindow Tester.
 */
public class TestRunExplorerTreeBuilderTest extends TestCase {
    private final CloverDatabase cloverDb;

    public TestRunExplorerTreeBuilderTest() throws Exception {
        final String path = getClass().getResource("/clover/coverage.db").getPath();
        cloverDb = new CloverDatabase(path);
        CoverageDataSpec spec = new CoverageDataSpec(new DefaultTestFilter(), 0);
        cloverDb.loadCoverageData(spec);

    }

    public void testLoad() {
        assertNotNull(cloverDb);
        assertTrue(cloverDb.getFullModel().hasTestResults());
    }

    public void testBuildTree() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        SortableListTreeTableModelOnColumns model = TreeTableModelFactory.getTestCasesTreeTableModel(null);
        model.setRoot(rootNode);
        TestRunExplorerTreeBuilder builder = new TestRunExplorerTreeBuilder(null, model, rootNode); // null -> no test sources view!

        builder.populate(cloverDb, cloverDb.getAppOnlyModel(), TestCaseLayout.PACKAGES, false, false);

        assertEquals(1, rootNode.getSiblingCount()); // a node is its own sibling in this context
        assertEquals(1, rootNode.getChildCount());
        rootNode.removeAllChildren();

        builder.populate(cloverDb, cloverDb.getAppOnlyModel(), TestCaseLayout.PACKAGES, true, false);

        assertEquals(1, rootNode.getSiblingCount());
        assertEquals(2, rootNode.getChildCount());

        DefaultMutableTreeNode parentPgkNode = findNamedNode(rootNode, "com.cenqua.clovertest");
        assertEquals(4, parentPgkNode.getChildCount());

        DefaultMutableTreeNode aOnlyTestNode = findNamedNode(parentPgkNode, "AOnlyTest");
        assertEquals(4, aOnlyTestNode.getChildCount());


        DefaultMutableTreeNode subpackageNode = findNamedNode(rootNode, "com.cenqua.clovertest.subpackage");
        assertEquals(1, subpackageNode.getChildCount());


        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);

        TestCaseInfo firstTest = (TestCaseInfo) ((DefaultMutableTreeNode) aOnlyTestNode.getFirstChild()).getUserObject();
        TestCaseInfo lastTest = (TestCaseInfo) ((DefaultMutableTreeNode) aOnlyTestNode.getLastChild()).getUserObject();
        assertEquals("testA1", firstTest.getTestName());
        assertEquals("testA99", lastTest.getTestName());

        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);

        firstTest = (TestCaseInfo) ((DefaultMutableTreeNode) aOnlyTestNode.getFirstChild()).getUserObject();
        lastTest = (TestCaseInfo) ((DefaultMutableTreeNode) aOnlyTestNode.getLastChild()).getUserObject();
        assertEquals("testA99", firstTest.getTestName());
        assertEquals("testA1", lastTest.getTestName());

        model.sortByColumn(3);
        TreeUtil.sortNodes(rootNode, model);

        assertEquals(4, aOnlyTestNode.getChildCount());
        //execution time-based test - not deterministic
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> children = aOnlyTestNode.children();
        double prevTime = 0;
        StringBuilder sb = new StringBuilder();
        while (children.hasMoreElements()) {
            final DefaultMutableTreeNode node = children.nextElement();
            final TestCaseInfo tci = (TestCaseInfo) node.getUserObject();
            sb.append("Test ").append(tci.getTestName()).append(", duration=").append(tci.getDuration());
            assertTrue(sb.toString(), prevTime <= tci.getDuration());
            prevTime = tci.getDuration();
        }

        model.sortByColumn(3);
        TreeUtil.sortNodes(rootNode, model);
        //execution time-based test - again not deterministic
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> childrenRev = aOnlyTestNode.children();
        prevTime = Float.MAX_VALUE;
        sb = new StringBuilder();
        while (childrenRev.hasMoreElements()) {
            final DefaultMutableTreeNode node = childrenRev.nextElement();
            final TestCaseInfo tci = (TestCaseInfo) node.getUserObject();
            sb.append("Test ").append(tci.getTestName()).append(", duration=").append(tci.getDuration());
            assertTrue(sb.toString(), prevTime >= tci.getDuration());
            prevTime = tci.getDuration();
        }


    }

    public void testBuildFlat() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        SortableListTreeTableModelOnColumns model = TreeTableModelFactory.getTestCasesTreeTableModel(null);
        model.setRoot(rootNode);
        TestRunExplorerTreeBuilder builder = new TestRunExplorerTreeBuilder(null, model, rootNode); // null -> no test sources view!

        final int totalTests = cloverDb.getCoverageData().getTests().size();

        builder.populate(cloverDb, cloverDb.getAppOnlyModel(), TestCaseLayout.TEST_CASES, true, false);

        assertEquals(1, rootNode.getSiblingCount()); // a node is its own sibling in this context
        assertEquals(totalTests, rootNode.getChildCount());

        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);

        TestCaseInfo firstTest = (TestCaseInfo) ((DefaultMutableTreeNode) rootNode.getFirstChild()).getUserObject();
        TestCaseInfo lastTest = (TestCaseInfo) ((DefaultMutableTreeNode) rootNode.getLastChild()).getUserObject();
        assertEquals("testA1", firstTest.getTestName());
        assertEquals("testMethod1", lastTest.getTestName());

        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);

        firstTest = (TestCaseInfo) ((DefaultMutableTreeNode) rootNode.getFirstChild()).getUserObject();
        lastTest = (TestCaseInfo) ((DefaultMutableTreeNode) rootNode.getLastChild()).getUserObject();
        assertEquals("testMethod1", firstTest.getTestName());
        assertEquals("testA1", lastTest.getTestName());

    }

    public static DefaultMutableTreeNode findNamedNode(DefaultMutableTreeNode root, String name) {
        Enumeration children = root.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            if (name.equals(((HasMetrics) (node.getUserObject())).getName())) {
                return node;
            }
        }
        return null;
    }

}
