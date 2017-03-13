package com.atlassian.clover.idea;

import com.atlassian.clover.idea.coverage.CoverageTreeModel;

public interface NodeWrapperSelectionListener {
    void elementSelected(CoverageTreeModel.NodeWrapper nodeWrapper);
}
