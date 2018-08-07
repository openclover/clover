package com.atlassian.clover.idea.treetables;

import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.util.ui.ColumnInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.List;

public class SortableListTreeTableModelOnColumns extends ListTreeTableModelOnColumns {

    private final ModelSorter modelSorter;

    public SortableListTreeTableModelOnColumns(TreeNode treeNode, ColumnInfo[] columnInfos, ModelSorter modelSorter) {
        super(treeNode, columnInfos);
        this.modelSorter = modelSorter;
    }

    @Override
    public boolean isSortable() {
        return true;
    }

    public void sortNodes(List<? extends TreeNode> nodes) {
        modelSorter.sortNodes(nodes);
    }

    public void sortByColumn(int sortBy) {
        modelSorter.sortByColumn(sortBy);
    }
}
