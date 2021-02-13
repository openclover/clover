package com.atlassian.clover.eclipse.core.views.widgets.columns;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

abstract class ColumnDefinitionsProvider implements IStructuredContentProvider {
    private ColumnDefinitionsModel model;

    @Override
    public abstract Object[] getElements(Object root);

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        this.model = (ColumnDefinitionsModel)newInput;
    }

    @Override
    public void dispose() {}
}
