package org.openclover.idea.treetables;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.entities.PackageFragment;
import org.openclover.idea.coverage.CoverageTreeModel;
import org.openclover.idea.testexplorer.SourceFolderDescription;
import org.openclover.idea.util.ui.CloverIcons;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Component;
import java.awt.Graphics;

public class ProjectTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        this.tree = tree;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        if (userObject instanceof CoverageTreeModel.NodeWrapper) {
            userObject = ((CoverageTreeModel.NodeWrapper) userObject).getHasMetrics();
        }
        if (userObject instanceof TestCaseInfo) {
            final TestCaseInfo tci = (TestCaseInfo) userObject;
            setIcon(CellRendererUtil.getIconForTestCaseInfo(tci));
            setText(tci.getTestName());
        } else if (userObject instanceof PackageFragment || userObject instanceof PackageInfo) {
            final HasMetrics packageInfo = (HasMetrics) userObject;
            setIcon(expanded ? CloverIcons.PACKAGE_OPEN : CloverIcons.PACKAGE_CLOSED);
            //LeftToRightOverride - fool JLabel to put ellipsis on the left hand side of text
            final String mangledName = new StringBuilder(packageInfo.getName()).append('\u202e').reverse().toString();
            setText(mangledName);

        } else if (userObject instanceof ClassInfo) {
            final ClassInfo classInfo = (ClassInfo) userObject;
            setIcon(CellRendererUtil.getIconForClassInfo(classInfo));
            setText(classInfo.getName());
        } else if (userObject instanceof SourceFolderDescription) {
            final SourceFolderDescription sourceFolder = (SourceFolderDescription) userObject;
            setText(sourceFolder.getName());
            if (sourceFolder.isTestFolder()) {
                setIcon(expanded ? CloverIcons.TEST_ROOT_FOLDER_OPEN : CloverIcons.TEST_ROOT_FOLDER);
            } else {
                setIcon(expanded ? CloverIcons.SOURCE_ROOT_FOLDER_OPEN : CloverIcons.SOURCE_ROOT_FOLDER);
            }
        } else if (userObject instanceof MethodInfo) {
            final MethodInfo methodInfo = (MethodInfo) userObject;
            setIcon(CellRendererUtil.getIconForMethodInfo(methodInfo));
            setText(methodInfo.getName());
        } else {
            setText(userObject != null ? userObject.toString() : "ERROR");
            setIcon(null);
        }

        return this;
    }

    private JTree tree;

    /**
     * Adjust JLabel size.
     */
    @Override
    public void paint(Graphics g) {
        final int maxWidth = tree.getWidth() - getBounds().x;
        if (maxWidth < getWidth()) {
            setSize(maxWidth, getHeight());
        }
        super.paint(g);
    }
}
