package org.openclover.idea.report.jfc;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.idea.coverage.CoverageTreeFilter;
import org.openclover.idea.coverage.CoverageTreeModel;

import javax.swing.tree.DefaultMutableTreeNode;

public class FullyCoveredFilter implements CoverageTreeFilter {
    @Override
    public boolean accept(DefaultMutableTreeNode aNode) {
        Object userObject = aNode.getUserObject();
        if (userObject instanceof CoverageTreeModel.NodeWrapper) {
            CoverageTreeModel.NodeWrapper nodeWrapper = (CoverageTreeModel.NodeWrapper) userObject;
            HasMetrics metrics = nodeWrapper.getHasMetrics();
            if (metrics != null) {
                return metrics.getMetrics().getNumUncoveredElements() != 0;
            }
        }
        return true;
    }
}
