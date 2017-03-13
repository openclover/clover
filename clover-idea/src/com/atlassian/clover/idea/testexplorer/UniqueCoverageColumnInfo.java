package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.idea.treetables.AbstractColumnInfo;
import com.atlassian.clover.idea.util.ComparatorUtil;

import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public class UniqueCoverageColumnInfo extends AbstractColumnInfo<Float> {

    private static final String COLUMN_NAME = "Unique Coverage";
    private static final String COLUMN_SIZER = COLUMN_NAME + "  ";


    public UniqueCoverageColumnInfo() {
        super(COLUMN_NAME, PercentBarTableCellRenderer.getInstance());
    }

    @Override
    public Float valueOf(DefaultMutableTreeNode defaultMutableTreeNode) {
        final Object userObject = defaultMutableTreeNode.getUserObject();
        if (userObject instanceof HasCoverageInfo) {
            final HasCoverageInfo coverageData = (HasCoverageInfo) userObject;
            return coverageData.getUniqueCoverage();
        } else {
            return null;
        }
    }

    /**
     * Force fixed column width.
     */
    @Override
    public int getWidth(JTable jTable) {
        return getWidth(jTable, COLUMN_SIZER);
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return this;
    }

    /**
     * Makes the package node stay in place when comparing (sorting) with class nodes.
     *
     * @param node1 node1
     * @param node2 node2
     * @return comparison result
     */
    @Override
    public int compare(DefaultMutableTreeNode node1, DefaultMutableTreeNode node2) {
        return ComparatorUtil.compareNE(valueOf(node1), valueOf(node2));
    }
}