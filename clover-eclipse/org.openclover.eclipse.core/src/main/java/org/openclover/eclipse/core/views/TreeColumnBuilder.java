package org.openclover.eclipse.core.views;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

public class TreeColumnBuilder {
    private ColumnDefinition columnDefinition;

    public TreeColumnBuilder(ColumnDefinition columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    public ColumnDefinition getColumnDefinition() {
        return columnDefinition;
    }

    public TreeColumn buildTreeColumn(Tree tree, TreeColumnLabeler labeler) {
        TreeColumn column = new TreeColumn(tree, columnDefinition.getAlignment());
        column.setText(labeler.columnTitleFor(column, columnDefinition));
        column.setToolTipText(columnDefinition.getTooltip());
        column.setMoveable(columnDefinition.isLocked());
        return column;

    }
}
