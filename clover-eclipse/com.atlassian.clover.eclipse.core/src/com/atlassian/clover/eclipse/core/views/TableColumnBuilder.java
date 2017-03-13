package com.atlassian.clover.eclipse.core.views;

import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Table;

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
