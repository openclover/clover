package com.atlassian.clover.idea.report.jfc;

import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.treetables.CellRendererUtil;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.FullProjectInfo;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Graphics;

public class HasMetricsTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public DefaultTreeCellRenderer getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        final HasMetrics hasMetrics;
        if (value instanceof DefaultMutableTreeNode) {
            hasMetrics = (HasMetrics) ((DefaultMutableTreeNode) value).getUserObject();
        } else {
            hasMetrics = (HasMetrics) value;
        }

        if (hasMetrics instanceof FullProjectInfo) {
            setIcon(CloverIcons.IDEA_PROJECT);
            setText(hasMetrics.getName());
        } else if (hasMetrics instanceof MethodInfo) {
            MethodInfo methodInfo = (MethodInfo) hasMetrics;
            setIcon(CellRendererUtil.getIconForMethodInfo(methodInfo));
            setText(hasMetrics.getName());
        } else if (hasMetrics instanceof BaseClassInfo) {
            final BaseClassInfo classInfo = (BaseClassInfo) hasMetrics;
            setIcon(CellRendererUtil.getIconForClassInfo(classInfo));
            setText(hasMetrics.getName());
        } else if (hasMetrics != null) {
            setIcon(leaf ? null : expanded ? CloverIcons.PACKAGE_OPEN : CloverIcons.PACKAGE_CLOSED);
            //LeftToRightOverride - fool JLabel to put ellipsis on the left hand side of text
            final String mangledName = new StringBuilder(hasMetrics.getName()).append('\u202e').reverse().toString();
            setText(mangledName);
        }

        return this;
    }

    /**
     * Adjust JLabel size to clip rect size, as this is actually tha space to be filled by the renderer.
     */
    @Override
    public void paint(Graphics g) {
        final int maxWidth = g.getClipBounds().width;
        if (maxWidth < getWidth()) {
            setSize(maxWidth, getHeight());
        }
        super.paint(g);
    }

}
