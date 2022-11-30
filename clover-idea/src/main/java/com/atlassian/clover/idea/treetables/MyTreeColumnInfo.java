package com.atlassian.clover.idea.treetables;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;

public abstract class MyTreeColumnInfo extends ColumnInfo {

    public MyTreeColumnInfo(String s) {
        super(s);
    }

    @Override
    public final Class getColumnClass() {
        return TreeTableModel.class;
    }

    @Override
    public Object valueOf(Object o) {
        return o;
    }
}
