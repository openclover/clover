package com.atlassian.clover.idea.util.ui;

import com.atlassian.clover.idea.treetables.SortableListTreeTableModelOnColumns;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TreeUtil {
    private TreeUtil() {
    }

    public static void sortNodes(final DefaultMutableTreeNode node, final SortableListTreeTableModelOnColumns tableModel) {
        List<DefaultMutableTreeNode> childrenArray = new ArrayList<DefaultMutableTreeNode>(node.getChildCount());
        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> children = node.children();
        while (children.hasMoreElements()) {
            childrenArray.add(children.nextElement());
        }

        tableModel.sortNodes(childrenArray);
        node.removeAllChildren(); // without this the loop would work fine, but it would be O(n^2) - see add() source
        for (DefaultMutableTreeNode child : childrenArray) {
            node.add(child);
        }

        for (DefaultMutableTreeNode child : childrenArray) {
            if (!child.isLeaf()) {
                sortNodes(child, tableModel);
            }
        }

    }
}
