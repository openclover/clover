package com.atlassian.clover.idea.coverageview.table;

import com.atlassian.clover.idea.coverageview.AbstractHasMetricsNodeComparator;
import com.atlassian.clover.idea.treetables.MyTreeColumnInfo;
import com.atlassian.clover.idea.util.ComparatorUtil;
import com.atlassian.clover.api.registry.HasMetrics;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public class ProjectTreeColumnInfo extends MyTreeColumnInfo {

    public ProjectTreeColumnInfo() {
        super("Element");
    }

    @Override
    public String getPreferredStringValue() {
        return "XXXXXXXXXXXXXXXXXXXXXXXXX";
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return COMPARATOR;
    }

    private final Comparator<DefaultMutableTreeNode> COMPARATOR = new AbstractHasMetricsNodeComparator() {

        @Override
        protected int compare(HasMetrics hm1, HasMetrics hm2) {
            final String value1 = hm1.getName();
            final String value2 = hm2.getName();

            return ComparatorUtil.compare(value1, value2);
        }
    };
}
