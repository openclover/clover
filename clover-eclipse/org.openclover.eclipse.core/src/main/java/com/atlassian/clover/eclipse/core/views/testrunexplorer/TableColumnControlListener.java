package com.atlassian.clover.eclipse.core.views.testrunexplorer;

import com.atlassian.clover.eclipse.core.views.ColumnControlListener;
import com.atlassian.clover.eclipse.core.views.ColumnCollectionSettings;
import com.atlassian.clover.eclipse.core.views.ColumnDefinition;
import com.atlassian.clover.eclipse.core.views.widgets.ColumnController;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.TableColumn;

public class TableColumnControlListener extends ColumnControlListener {
    private TableColumn column;

    public TableColumnControlListener(ColumnController target, ColumnCollectionSettings settings, TableColumn column, int initialColumnIndex, ColumnDefinition columnDef) {
        super(target, settings, initialColumnIndex, columnDef);
        this.column = column;
    }

    @Override
    public void controlMoved(ControlEvent controlEvent) {
        settings.setColumnOrder(column.getParent().getColumnOrder());
    }

    @Override
    public void controlResized(ControlEvent controlEvent) {
        settings.setVisibleColumnSize(columnDef, column.getWidth());
    }
}
