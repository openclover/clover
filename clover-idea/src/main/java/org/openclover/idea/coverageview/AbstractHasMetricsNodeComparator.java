package org.openclover.idea.coverageview;

import org.openclover.core.api.registry.HasMetrics;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public abstract class AbstractHasMetricsNodeComparator implements Comparator<DefaultMutableTreeNode> {
    @Override
    public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
        final Object userObject1 = o1.getUserObject();
        final Object userObject2 = o2.getUserObject();

        final HasMetrics hm1 = userObject1 instanceof HasMetrics ? (HasMetrics) userObject1 : null;
        final HasMetrics hm2 = userObject2 instanceof HasMetrics ? (HasMetrics) userObject2 : null;

        if (hm1 == hm2) {
            return 0;
        }

        if (hm1 == null) {
            return -1;
        }

        if (hm2 == null) {
            return 1;
        }

        return compare(hm1, hm2);

    }

    protected abstract int compare(HasMetrics hm1, HasMetrics hm2);
}
