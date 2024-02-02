package org.openclover.idea.coverageview.table;

import org.openclover.idea.coverage.CoverageTreeModel;
import org.openclover.idea.treetables.AbstractColumnInfo;
import com.atlassian.clover.api.registry.HasMetrics;

import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public abstract class AbstractHasMetricsColumnInfo<T> extends AbstractColumnInfo<T> {
    public AbstractHasMetricsColumnInfo(String columnName, TableCellRenderer cellRenderer) {
        super(columnName, cellRenderer);
    }

    public AbstractHasMetricsColumnInfo(String columnName) {
        super(columnName);
    }

    @Override
    public T valueOf(DefaultMutableTreeNode defaultMutableTreeNode) {
        final Object userObject = defaultMutableTreeNode.getUserObject();
        final HasMetrics hasMetrics;
        if (userObject instanceof CoverageTreeModel.NodeWrapper) {
            hasMetrics = ((CoverageTreeModel.NodeWrapper) userObject).getHasMetrics();
        } else if (userObject instanceof HasMetrics) {
            hasMetrics = (HasMetrics) userObject;
        } else {
            return null;
        }
        return getValue(hasMetrics);
    }

    protected abstract T getValue(HasMetrics hasMetrics);

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        /* use the default comparator provided in the superclass */
        return this;
    }

}
