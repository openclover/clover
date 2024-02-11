package org.openclover.idea.testexplorer;

import com.atlassian.clover.CloverDatabase;
import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.CoverageDataSpec;
import org.openclover.idea.treetables.SortableListTreeTableModelOnColumns;
import org.openclover.idea.treetables.TreeTableModelFactory;
import org.openclover.idea.util.ui.TreeUtil;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import junit.framework.TestCase;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * CoverageContributionPanel Tester.
 */
public class CoverageContributionTreeBuilderTest extends TestCase {
    private final CloverDatabase cloverDb;
    private DefaultMutableTreeNode rootNode;
    private SortableListTreeTableModelOnColumns model;
    private CoverageContributionTreeBuilder builder;

    public CoverageContributionTreeBuilderTest() throws CloverException {
        final String path = getClass().getResource("/clover/coverage.db").getPath();
        cloverDb = new CloverDatabase(path);
        CoverageDataSpec spec = new CoverageDataSpec(new DefaultTestFilter(), 0);
        cloverDb.loadCoverageData(spec);

    }

    @Override
    protected void setUp() throws Exception {
        rootNode = new DefaultMutableTreeNode();
        model = TreeTableModelFactory.getTestCaseInfoTreeTableModel(rootNode);
        builder = new CoverageContributionTreeBuilder(rootNode, model);
    }

    @Override
    protected void tearDown() throws Exception {
        rootNode = null;
        model = null;
        builder = null;
    }

    public void testLoad() {
        assertNotNull(cloverDb);
        assertTrue(cloverDb.getFullModel().hasTestResults());
    }

    public void testSort() {
        TestCaseInfo troublesome = findTestCase(cloverDb, "testAB");
        builder.processClassesFor(cloverDb, troublesome, true);

        assertEquals(1, rootNode.getChildCount());

        DefaultMutableTreeNode pkgNode = (DefaultMutableTreeNode) rootNode.getChildAt(0);
        FullPackageInfo pkg = (FullPackageInfo) pkgNode.getUserObject();
        assertEquals("com.cenqua.clovertest", pkg.getName());
        assertEquals(2, pkgNode.getChildCount());

        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);

        DefaultMutableTreeNode firstClassNode = (DefaultMutableTreeNode) pkgNode.getChildAt(0);
        DefaultMutableTreeNode secondClassNode = (DefaultMutableTreeNode) pkgNode.getChildAt(1);

        CoverageDataHolder firstClass = (CoverageDataHolder) firstClassNode.getUserObject();
        CoverageDataHolder secondClass = (CoverageDataHolder) secondClassNode.getUserObject();

        assertEquals("A", firstClass.getElement().getName());
        assertEquals("B", secondClass.getElement().getName());

        assertEquals(2, firstClassNode.getChildCount());
        assertEquals(1, secondClassNode.getChildCount());

        CoverageDataHolder m1 = (CoverageDataHolder) ((DefaultMutableTreeNode) firstClassNode.getChildAt(0)).getUserObject();
        CoverageDataHolder m2 = (CoverageDataHolder) ((DefaultMutableTreeNode) firstClassNode.getChildAt(1)).getUserObject();

        assertEquals("a1() : void", m1.getElement().getName());
        assertEquals("a2() : void", m2.getElement().getName());

        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);

        firstClassNode = (DefaultMutableTreeNode) pkgNode.getChildAt(0);
        secondClassNode = (DefaultMutableTreeNode) pkgNode.getChildAt(1);

        firstClass = (CoverageDataHolder) firstClassNode.getUserObject();
        secondClass = (CoverageDataHolder) secondClassNode.getUserObject();

        assertEquals("B", firstClass.getElement().getName());
        assertEquals("A", secondClass.getElement().getName());

        assertEquals(1, firstClassNode.getChildCount());
        assertEquals(2, secondClassNode.getChildCount());

        m1 = (CoverageDataHolder) ((DefaultMutableTreeNode) secondClassNode.getChildAt(0)).getUserObject();
        m2 = (CoverageDataHolder) ((DefaultMutableTreeNode) secondClassNode.getChildAt(1)).getUserObject();

        assertEquals("a2() : void", m1.getElement().getName());
        assertEquals("a1() : void", m2.getElement().getName());
    }

    public void testFlat() {
        TestCaseInfo troublesome = findTestCase(cloverDb, "testAB");
        builder.processClassesFor(cloverDb, troublesome, false);

        DefaultMutableTreeNode pkgNode = (DefaultMutableTreeNode) rootNode.getChildAt(0).getChildAt(0).getChildAt(0);
        SimplePackageFragment pkg = (SimplePackageFragment) pkgNode.getUserObject();

        assertEquals("clovertest", pkg.getName());
        assertEquals(2, pkgNode.getChildCount());

    }

    public void testMixedElementsSort() {
        TestCaseInfo troublesome = findTestCase(cloverDb, "testAM");
        builder.processClassesFor(cloverDb, troublesome, false);

        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);
        verifyAMStructure(false);

        model.sortByColumn(0);
        TreeUtil.sortNodes(rootNode, model);
        verifyAMStructure(true);

        model.sortByColumn(1);
        TreeUtil.sortNodes(rootNode, model);
        verifyAMStructure(false);

        model.sortByColumn(1);
        TreeUtil.sortNodes(rootNode, model);
        verifyAMStructure(true);

        // testAM is no longer suitable for unique sort test
        rootNode.removeAllChildren();
        TestCaseInfo hasUnique = findTestCase(cloverDb, "testAB");
        builder.processClassesFor(cloverDb, hasUnique, false);

        model.sortByColumn(0);

        model.sortByColumn(2);
        TreeUtil.sortNodes(rootNode, model);
        verifyABStructure(false);

        model.sortByColumn(2);
        TreeUtil.sortNodes(rootNode, model);
        verifyABStructure(true);
    }

    private void verifyAMStructure(boolean reverse) {
        DefaultMutableTreeNode clovertestNode = (DefaultMutableTreeNode) rootNode.getChildAt(0).getChildAt(0).getChildAt(0);
        SimplePackageFragment clovertestPkg = (SimplePackageFragment) clovertestNode.getUserObject();
        DefaultMutableTreeNode subpackageNode = (DefaultMutableTreeNode) clovertestNode.getFirstChild();
        SimplePackageFragment subPkg = (SimplePackageFragment) subpackageNode.getUserObject();

        assertEquals("clovertest", clovertestPkg.getName());
        assertEquals("subpackage", subPkg.getName());

        final TreeNode mClass = subpackageNode.getFirstChild();
        CoverageDataHolder m1 = (CoverageDataHolder) ((DefaultMutableTreeNode) mClass.getChildAt(reverse ? 1 : 0)).getUserObject();
        CoverageDataHolder m2 = (CoverageDataHolder) ((DefaultMutableTreeNode) mClass.getChildAt(reverse ? 0 : 1)).getUserObject();

        assertEquals("m1() : void", m1.getElement().getName());
        assertEquals("m2() : void", m2.getElement().getName());
    }

    private void verifyABStructure(boolean reverse) {
        DefaultMutableTreeNode clovertestNode = (DefaultMutableTreeNode) rootNode.getChildAt(0).getChildAt(0).getChildAt(0);
        SimplePackageFragment clovertestPkg = (SimplePackageFragment) clovertestNode.getUserObject();

        assertEquals("clovertest", clovertestPkg.getName());

        final TreeNode class1Node = clovertestNode.getFirstChild();
        final TreeNode class2Node = clovertestNode.getLastChild();

        CoverageDataHolder classA = (CoverageDataHolder) ((DefaultMutableTreeNode) (reverse ? class2Node : class1Node)).getUserObject();
        CoverageDataHolder classB = (CoverageDataHolder) ((DefaultMutableTreeNode) (reverse ? class1Node : class2Node)).getUserObject();

        assertEquals("A", classA.getElement().getName());
        assertEquals("B", classB.getElement().getName());
    }


    private static TestCaseInfo findTestCase(CloverDatabase db, String name) {
        for (TestCaseInfo tci : db.getCoverageData().getTests()) {
            if (name.equals(tci.getTestName())) {
                return tci;
            }
        }
        return null;
    }
}

