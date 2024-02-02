package com.atlassian.clover.idea.treetables;

import com.atlassian.clover.idea.coverageview.table.ComplexityColumnInfo;
import com.atlassian.clover.idea.coverageview.table.CoverageColumnInfo;
import com.atlassian.clover.idea.coverageview.table.LOCColumnInfo;
import com.atlassian.clover.idea.coverageview.table.ProjectTreeColumnInfo;
import com.atlassian.clover.idea.coverageview.table.UncoveredColumnInfo;
import com.atlassian.clover.idea.testexplorer.ClassTreeColumnInfo;
import com.atlassian.clover.idea.testexplorer.ContribCoverageColumnInfo;
import com.atlassian.clover.idea.testexplorer.TestExecutionTimeColumnInfo;
import com.atlassian.clover.idea.testexplorer.TestMessageColumnInfo;
import com.atlassian.clover.idea.testexplorer.TestStartedColumnInfo;
import com.atlassian.clover.idea.testexplorer.TestStatusColumnInfo;
import com.atlassian.clover.idea.testexplorer.TestTreeColumnInfo;
import com.atlassian.clover.idea.testexplorer.UniqueCoverageColumnInfo;
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
