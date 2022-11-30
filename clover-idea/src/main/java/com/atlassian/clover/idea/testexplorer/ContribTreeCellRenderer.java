package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.idea.report.jfc.HasMetricsTreeCellRenderer;
import com.atlassian.clover.api.registry.HasMetrics;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;

public class ContribTreeCellRenderer implements TreeCellRenderer {
    private static final HasMetricsTreeCellRenderer HMTCR = new HasMetricsTreeCellRenderer();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        final HasMetrics hasMetrics;
        Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
        if (userObject instanceof HasMetrics) {
            hasMetrics = (HasMetrics) userObject;
        } else if (userObject instanceof CoverageDataHolder) {
            CoverageDataHolder coverageData = (CoverageDataHolder) userObject;
            hasMetrics = coverageData.getElement();
        } else {
            hasMetrics = null;
        }

        return HMTCR.getTreeCellRendererComponent(tree, hasMetrics, selected, expanded, leaf, row, hasFocus);
    }
}
