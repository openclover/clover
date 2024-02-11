package org.openclover.idea.util.ui;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;
import java.util.Set;

import static org.openclover.core.util.Sets.newHashSet;

public class TreeExpansionHelper extends TreeSelectionHelper {
    private final Set<Object> expandedElements = newHashSet();

    public TreeExpansionHelper(JTree origTree) {
        super(origTree);

        TreePath rootPath = new TreePath(origTree.getModel().getRoot());
        final Enumeration<TreePath> expanded = origTree.getExpandedDescendants(rootPath);
        while (expanded != null && expanded.hasMoreElements()) {
            TreePath treePath = expanded.nextElement();
            DefaultMutableTreeNode element = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            if (element.getParent() != null) {
                // ignore the root element
                expandedElements.add(getWrapperObject(element.getUserObject()));
            }
        }
    }

    /**
     * Also restores selection - no need to call {@link #restore(javax.swing.JTree)}
     *
     * @param tree tree to restore expansion and selection on.
     */
    @Override
    public void restore(JTree tree) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration children = root.depthFirstEnumeration();
        TreePath selectionPath = null;
        while (children.hasMoreElements()) {
            final DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            final Object wrappedObject = getWrapperObject(child.getUserObject());

            if (expandedElements.contains(wrappedObject)) {
                TreePath tp = new TreePath(child.getPath());
                tree.expandPath(tp);
            }
            if (selectedElement.equals(wrappedObject)) {
                selectionPath = new TreePath(child.getPath());
            }
        }
        if (selectionPath != null) {
            tree.setSelectionPath(selectionPath);
        }
    }
}
