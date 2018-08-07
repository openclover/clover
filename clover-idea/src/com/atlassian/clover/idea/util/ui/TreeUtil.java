package com.atlassian.clover.idea.util.ui;

import com.atlassian.clover.idea.treetables.SortableListTreeTableModelOnColumns;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TreeUtil {
    private TreeUtil() {
    }

    public static void sortNodes(final DefaultMutableTreeNode node, final SortableListTreeTableModelOnColumns tableModel) {
        List<TreeNode> childrenArray = new ArrayList<TreeNode>(node.getChildCount());

        Enumeration<TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            childrenArray.add(children.nextElement());
        }

        tableModel.sortNodes(childrenArray);
        node.removeAllChildren(); // without this the loop would work fine, but it would be O(n^2) - see add() source
        for (TreeNode child : childrenArray) {
            node.add((DefaultMutableTreeNode) child);
        }

        for (TreeNode child : childrenArray) {
            if (!child.isLeaf()) {
                sortNodes((DefaultMutableTreeNode) child, tableModel);
            }
        }

    }
}
