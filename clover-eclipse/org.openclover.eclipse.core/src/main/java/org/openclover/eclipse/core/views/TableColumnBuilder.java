package org.openclover.eclipse.core.views;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public abstract class TableColumnBuilder {
    private int id;

    public TableColumnBuilder(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public abstract TableColumn buildTableColumn(Table table);
}
