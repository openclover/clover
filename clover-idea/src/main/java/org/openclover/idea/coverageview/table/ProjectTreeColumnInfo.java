package org.openclover.idea.coverageview.table;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.idea.coverageview.AbstractHasMetricsNodeComparator;
import org.openclover.idea.treetables.MyTreeColumnInfo;
import org.openclover.idea.util.ComparatorUtil;

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
