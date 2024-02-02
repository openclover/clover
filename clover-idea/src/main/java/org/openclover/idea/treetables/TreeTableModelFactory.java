package org.openclover.idea.treetables;

import org.openclover.idea.coverageview.table.ComplexityColumnInfo;
import org.openclover.idea.coverageview.table.CoverageColumnInfo;
import org.openclover.idea.coverageview.table.LOCColumnInfo;
import org.openclover.idea.coverageview.table.ProjectTreeColumnInfo;
import org.openclover.idea.coverageview.table.UncoveredColumnInfo;
import org.openclover.idea.testexplorer.ClassTreeColumnInfo;
import org.openclover.idea.testexplorer.ContribCoverageColumnInfo;
import org.openclover.idea.testexplorer.TestExecutionTimeColumnInfo;
import org.openclover.idea.testexplorer.TestMessageColumnInfo;
import org.openclover.idea.testexplorer.TestStartedColumnInfo;
import org.openclover.idea.testexplorer.TestStatusColumnInfo;
import org.openclover.idea.testexplorer.TestTreeColumnInfo;
import org.openclover.idea.testexplorer.UniqueCoverageColumnInfo;
import com.intellij.util.ui.ColumnInfo;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TreeTableModelFactory {
    private static final ColumnInfo[] COVERAGE_COLUMN_INFOS = {
            new ProjectTreeColumnInfo(),
            new CoverageColumnInfo(),
            new ComplexityColumnInfo(),
            new LOCColumnInfo(),
            new UncoveredColumnInfo()
    };

    private static final ColumnInfo[] TEST_CASE_INFO_COLUMN_INFOS = {
            new ClassTreeColumnInfo(),
            new ContribCoverageColumnInfo(),
            new UniqueCoverageColumnInfo()
    };

    private static final ColumnInfo[] TEST_CASES_COLUMN_INFOS = {
            new TestTreeColumnInfo(),
            new TestStartedColumnInfo(),
            new TestStatusColumnInfo(),
            new TestExecutionTimeColumnInfo(),
            new ContribCoverageColumnInfo(),
            new UniqueCoverageColumnInfo(),
            new TestMessageColumnInfo()
    };

    public static SortableListTreeTableModelOnColumns getTestCasesTreeTableModel(TreeNode treeNode) {
        return getSortableListTreeTableModelOnColumns(treeNode, TEST_CASES_COLUMN_INFOS);
    }

    public static SortableListTreeTableModelOnColumns getCoverageTreeTableModel(TreeNode treeNode) {
        return getSortableListTreeTableModelOnColumns(treeNode, COVERAGE_COLUMN_INFOS);
    }

    public static SortableListTreeTableModelOnColumns getTestCaseInfoTreeTableModel(DefaultMutableTreeNode treeNode) {
        return getSortableListTreeTableModelOnColumns(treeNode, TEST_CASE_INFO_COLUMN_INFOS);
    }

    public static SortableListTreeTableModelOnColumns getSortableListTreeTableModelOnColumns(TreeNode treeNode, ColumnInfo[] columnInfos) {
        final ModelSorter modelSorter = new ModelSorter(columnInfos);
        return new SortableListTreeTableModelOnColumns(treeNode, columnInfos, modelSorter);
    }

    public static JTree retrieveJTree(Component potentialTreeTableView) {
        try {
            final Method getTree = potentialTreeTableView.getClass().getMethod("getTree");
            Object o = getTree.invoke(potentialTreeTableView);
            return (o instanceof JTree) ? (JTree) o : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }

    }
}
