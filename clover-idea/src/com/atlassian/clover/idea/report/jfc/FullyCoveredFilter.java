package com.atlassian.clover.idea.report.jfc;

import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.idea.coverage.CoverageTreeFilter;
import com.atlassian.clover.idea.coverage.CoverageTreeModel;

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
