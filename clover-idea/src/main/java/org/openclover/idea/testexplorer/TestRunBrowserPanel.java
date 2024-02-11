package org.openclover.idea.testexplorer;

import org.openclover.core.CloverDatabase;
import org.openclover.idea.config.TestCaseLayout;
import org.openclover.idea.treetables.TreeTableModelFactory;
import org.openclover.idea.treetables.TreeTablePanel;
import org.openclover.idea.util.ui.TreeExpansionHelper;
import org.openclover.idea.util.ui.TreeSelectionHelper;
import org.openclover.core.registry.CoverageDataReceptor;
import org.openclover.core.registry.entities.TestCaseInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.ui.ColumnInfo;

import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

public class TestRunBrowserPanel extends TreeTablePanel {

    private final List<TestRunExplorerToolWindow.TestCaseSelectionListener> listeners = newArrayList();
    private final TestRunExplorerTreeBuilder treeBuilder;

    public TestRunBrowserPanel(Project project) {
        super(project, TreeTableModelFactory.getTestCasesTreeTableModel(null));
        treeBuilder = new TestRunExplorerTreeBuilder(project, tableModel, rootNode);

        treeTableView.getTree().addTreeSelectionListener(treeSelectionEvent -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeSelectionEvent.getPath().getLastPathComponent();
            updateTestCaseSelectionListeners(node);
        });

        add(new JScrollPane(treeTableView), BorderLayout.CENTER);
        indexCoverageColumns();
    }

    public void addTestCaseSelectionListener(TestRunExplorerToolWindow.TestCaseSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeTestCaseSelectionListener(TestRunExplorerToolWindow.TestCaseSelectionListener listener) {
        listeners.remove(listener);
    }

    private void updateTestCaseSelectionListeners(DefaultMutableTreeNode node) {
        final Object userObject = node == null ? null : node.getUserObject();
        TestCaseInfo tci = (TestCaseInfo) (userObject instanceof TestCaseInfo ? userObject : null);
        if (tci instanceof DecoratedTestCaseInfo) {
            // decoration breaks equals() and stuff, so strip before it leaks outside this panel
            tci = ((DecoratedTestCaseInfo) tci).getNakedTestCaseInfo();
        }
        for (TestRunExplorerToolWindow.TestCaseSelectionListener listener : listeners) {
            listener.valueChanged(tci);
        }
    }

    /**
     * Repopulate the tree
     *
     * @param cloverDatabase    clover database to retrieve test case infos from
     * @param receptor          only tests touching receptor will be displayed
     * @param calculateCoverage whether tree builder is supposed to calculate each test's coverage for given receptor
     * @param flatten           packages should be flattened
     * @param testCaseLayout    flat/hierarhical layout
     */
    public void update(CloverDatabase cloverDatabase, CoverageDataReceptor receptor, boolean calculateCoverage, boolean flatten, TestCaseLayout testCaseLayout) {
        final boolean customExpand = !alwaysExpandTestCases && !alwaysCollapseTestCases;
        final TreeSelectionHelper teh = customExpand ?
                new TreeExpansionHelper(treeTableView.getTree()) :
                new TreeSelectionHelper(treeTableView.getTree());

        rootNode.removeAllChildren();
        treeBuilder.cancelLastCalculator();
        if (cloverDatabase != null) {
            treeBuilder.populate(cloverDatabase, receptor, testCaseLayout, flatten, calculateCoverage);
        }
        tableModel.nodeStructureChanged(rootNode);
        if (!customExpand && alwaysExpandTestCases) {
            expandAll(true);
        }
        teh.restore(treeTableView.getTree());

        TreePath selection = treeTableView.getTree().getSelectionModel().getSelectionPath();
        if (selection == null && treeTableView.getRowCount() > 0) {
            selection = treeTableView.getTree().getPathForRow(treeTableView.getRowCount() - 1);
            treeTableView.getTree().getSelectionModel().setSelectionPath(selection);
        }
        updateTestCaseSelectionListeners(selection != null ? (DefaultMutableTreeNode) selection.getLastPathComponent() : null);
    }

    /**
     * Repopulate the tree
     *
     * @param cloverDatabase clover database to retrieve test case infos from
     * @param testCases      only selected tests will be displayed
     * @param flatten        packages should be flattened
     * @param testCaseLayout flat/hierarhical layout
     */
    public void update(CloverDatabase cloverDatabase, Collection<TestCaseInfo> testCases, boolean flatten, TestCaseLayout testCaseLayout) {
        rootNode.removeAllChildren();
        if (cloverDatabase != null) {
            treeBuilder.populate(cloverDatabase, testCases, testCaseLayout, flatten);
        }
        tableModel.nodeStructureChanged(rootNode);
        expandAll(true);
        treeTableView.getTree().setSelectionRow(treeTableView.getRowCount() - 1);
    }


    @Override
    public void clean() {
        super.clean();
        updateTestCaseSelectionListeners(null);
    }

    public void setAlwaysExpandTestCases(boolean newAlwaysExpandTestCases) {
        if (alwaysExpandTestCases != newAlwaysExpandTestCases) {
            alwaysExpandTestCases = newAlwaysExpandTestCases;
            expansionChanged();
        }
    }

    public void setAlwaysCollapseTestCases(boolean newAlwaysCollapseTestCases) {
        if (alwaysCollapseTestCases != newAlwaysCollapseTestCases) {
            alwaysCollapseTestCases = newAlwaysCollapseTestCases;
            expansionChanged();
        }
    }

    private void expansionChanged() {
        if (alwaysCollapseTestCases != alwaysExpandTestCases) {
            expandAll(alwaysExpandTestCases);
        }
    }

    private final Collection<Pair<Integer, TableColumn>> coverageColumns = newArrayList();

    private void indexCoverageColumns() {
        ColumnInfo[] ci = tableModel.getColumnInfos();
        for (int i = 0; i < ci.length; i++) {
            ColumnInfo columnInfo = ci[i];
            if (columnInfo instanceof ContribCoverageColumnInfo || columnInfo instanceof UniqueCoverageColumnInfo) {
                coverageColumns.add(Pair.create(i, treeTableView.getColumnModel().getColumn(i)));
            }
        }
    }

    public void showCoverageColumns(boolean show) {
        final TableColumnModel tableColumnModel = treeTableView.getColumnModel();
        for (Pair<Integer, TableColumn> tc : coverageColumns) {
            tableColumnModel.removeColumn(tc.getSecond());
            if (show) {
                tableColumnModel.addColumn(tc.getSecond());
                tableColumnModel.moveColumn(tableColumnModel.getColumnCount() - 1, tc.getFirst());
            }
        }
    }
}