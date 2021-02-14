package org.openclover.eclipse.core.views;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.openclover.eclipse.core.views.widgets.ColumnController;

public abstract class ColumnControlListener extends SelectionAdapter implements ControlListener {
    protected final ColumnCollectionSettings settings;
    protected final ColumnController controller;
    protected final int initialColumnIndex;
    protected final ColumnDefinition columnDef;

    public ColumnControlListener(ColumnController controller, ColumnCollectionSettings settings, int initialColumnIndex, ColumnDefinition columnDef) {
        this.settings = settings;
        this.controller = controller;
        this.initialColumnIndex = initialColumnIndex;
        this.columnDef = columnDef;
    }

    @Override
    public void widgetSelected(SelectionEvent selectionEvent) {
        settings.sortOn(columnDef);
        controller.syncSorting();
    }
}
