package org.openclover.idea.util.ui;

import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.entities.PackageFragment;
import org.openclover.idea.coverage.CoverageTreeModel;
import org.openclover.idea.testexplorer.DecoratedTestCaseInfo;
import org.openclover.idea.testexplorer.SimplePackageFragment;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

public class TreeSelectionHelper {
    final Object selectedElement;

    public TreeSelectionHelper(JTree origTree) {
        final TreePath currentSelection = origTree.getSelectionPath();
        final DefaultMutableTreeNode selectedNode = currentSelection == null ? null : (DefaultMutableTreeNode) currentSelection.getLastPathComponent();
        selectedElement = selectedNode == null ? new Object() : getWrapperObject(selectedNode.getUserObject());
    }

    public void restore(JTree tree) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration children = root.depthFirstEnumeration();
        TreePath selectionPath = null;
        while (children.hasMoreElements() && selectionPath == null) {
            final DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            final Object wrappedObject = getWrapperObject(child.getUserObject());

            if (selectedElement.equals(wrappedObject)) {
                selectionPath = new TreePath(child.getPath());
            }
        }
        if (selectionPath != null) {
            tree.setSelectionPath(selectionPath);
        }
    }


    /**
     * Wrap with an object with a working equals() method if necessary.
     * May return the same object if it already has valid equals().
     *
     * @param o object that needs to be equalable
     * @return object with a working equals()
     */
    static Object getWrapperObject(Object o) {
        if (o instanceof CoverageTreeModel.NodeWrapper) {
            return getWrapperObject(((CoverageTreeModel.NodeWrapper) o).getHasMetrics());
        } else if (o instanceof PackageFragment) {
            return new PackageWrapper((PackageFragment) o);
        } else if (o instanceof FullPackageInfo) {
            return new PackageWrapper((FullPackageInfo) o);
        } else if (o instanceof SimplePackageFragment) {
            return new PackageWrapper((SimplePackageFragment) o);
        } else if (o instanceof DecoratedTestCaseInfo) {
            return ((DecoratedTestCaseInfo) o).getNakedTestCaseInfo();
        } else {
            return o;
        }
    }

    private static class PackageWrapper {
        private final Object packageName;

        PackageWrapper(PackageFragment packageFragment) {
            this.packageName = packageFragment.getQualifiedName();
        }

        private PackageWrapper(FullPackageInfo packageInfo) {
            this.packageName = packageInfo.getName();
        }

        private PackageWrapper(SimplePackageFragment simplePackageFragment) {
            PackageInfo concrete = simplePackageFragment.getConcretePackage();

            this.packageName = concrete != null ? concrete.getName() : new Object();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PackageWrapper && packageName.equals(((PackageWrapper) obj).packageName);
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
        }
    }

}
