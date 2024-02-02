package org.openclover.idea.coverage;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Interface supports the implementation of a filter that can be added to the
 * CoverageTreeModel to produce a filtered version of the tree.
 * <p>Typical examples of a CoverageTreeFilter would include a filter to include
 * only coverage that is below a specified percentage. This is useful for showing
 * only those coverage elements that are in need of extra testing.
 */
public interface CoverageTreeFilter {

    /**
     *
     */
    boolean accept(DefaultMutableTreeNode aNode);

}
