package org.openclover.eclipse.core.views;

import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.openclover.eclipse.core.views.widgets.ColumnController;

/**
 * Listens to control resize events and updates settings accordingly
 */
public class TreeColumnControlListener extends ColumnControlListener {
    private TreeViewer treeViewer;
    private TreeColumn column;
    private TreeColumnLabeler labeler;

    public TreeColumnControlListener(ColumnController rebuilder, TreeColumnLabeler labeler, TreeViewer treeViewer, ColumnCollectionSettings settings, TreeColumn column, int initialColumnIndex, ColumnDefinition columnDef) {
        super(rebuilder, settings, initialColumnIndex, columnDef);
        this.labeler = labeler;
        this.treeViewer = treeViewer;
        this.column = column;
    }

    @Override
    public void controlMoved(ControlEvent controlEvent) {
        settings.setColumnOrder(column.getParent().getColumnOrder());
    }

    @Override
    public void controlResized(ControlEvent controlEvent) {
        column.setText(labeler.columnTitleFor(column,  columnDef));
        settings.setVisibleColumnSize(columnDef, column.getWidth());
    }

}
