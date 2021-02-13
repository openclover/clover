package com.atlassian.clover.eclipse.core.views;

public abstract class ColumnManager {
    public abstract ColumnDefinition[] getAllColumns();
    public abstract ColumnDefinition[] getVisibleColumns();
    public abstract void update(
        CustomColumnDefinition[] custom,
        ColumnDefinition[] selected);
}
