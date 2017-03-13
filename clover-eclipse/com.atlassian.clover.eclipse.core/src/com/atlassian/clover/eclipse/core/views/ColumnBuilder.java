package com.atlassian.clover.eclipse.core.views;

import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Tree;

public class ColumnBuilder {
    public static TreeColumn buildTreeColumn(ColumnDefinition definition, Tree tree, TreeColumnLabeler labeler) {
        TreeColumn column = new TreeColumn(tree, definition.getAlignment());
        column.setText(labeler.columnTitleFor(column, definition));
        column.setToolTipText(definition.getTooltip());
        column.setMoveable(!definition.isLocked());
        return column;
    }
}
