package org.openclover.idea;

import org.openclover.idea.coverage.CoverageTreeModel;

public interface NodeWrapperSelectionListener {
    void elementSelected(CoverageTreeModel.NodeWrapper nodeWrapper);
}
