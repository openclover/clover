package org.openclover.idea.report.jfc;


import org.openclover.idea.coverage.CoverageTreeModel;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;

public class ProjectJfcTreeCellRenderer implements TreeCellRenderer {
    private static final HasMetricsTreeCellRenderer HMTCR = new HasMetricsTreeCellRenderer();
    private static final DefaultTreeCellRenderer DEFAULT_RENDERER = new DefaultTreeCellRenderer();

    static {
        DEFAULT_RENDERER.setOpenIcon(null);
        DEFAULT_RENDERER.setClosedIcon(null);
        DEFAULT_RENDERER.setLeafIcon(null);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object obj = node.getUserObject();
        if (obj instanceof CoverageTreeModel.NodeWrapper) {
            CoverageTreeModel.NodeWrapper wrapper = (CoverageTreeModel.NodeWrapper) obj;
            JLabel component = HMTCR.getTreeCellRendererComponent(tree,
                                                                  wrapper.getHasMetrics(),
                                                                  selected, expanded, leaf, row, hasFocus);
            component.setText(wrapper.toString());
            return component;
        } else {
            // rely on toString()
            return DEFAULT_RENDERER.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
        }
    }
}
