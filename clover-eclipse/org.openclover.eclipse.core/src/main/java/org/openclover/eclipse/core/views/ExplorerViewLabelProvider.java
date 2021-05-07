package org.openclover.eclipse.core.views;

import org.openclover.eclipse.core.projects.CloveredProjectLabelProvider;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Provides images and text for coverage table in CoverageView.
 */
public abstract class ExplorerViewLabelProvider implements ITableLabelProvider {
    /**
     * Delegates to standard workbench version for defaults
     */
    protected ILabelProvider delegate = new CloveredProjectLabelProvider();
    protected ColumnCollectionSettings columnSettings;
    protected ExplorerViewSettings settings;

    public ExplorerViewLabelProvider(ExplorerViewSettings settings, ColumnCollectionSettings columnSettings) {
        this.settings = settings;
        this.columnSettings = columnSettings;
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        ColumnDefinition column = columnSettings.columnForIndex(columnIndex);
        if (column == null) {
            return delegate.getImage(element);
        } else if (column.displaysImage()) {
            return column.getImage(settings, getMetricsScope(), delegate, element);
        } else {
            return null;
        }
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        ColumnDefinition column = columnSettings.columnForIndex(columnIndex);
        if (column == null) {
            return delegate.getText(element);
        } else if (column.displaysSimpleLabel()) {
            return column.getLabel(settings, getMetricsScope(), delegate, element);
        } else {
            return null;
        }
    }

    /**
     * Delegates
     */
    @Override
    public void addListener(ILabelProviderListener listener) {
        delegate.addListener(listener);
    }

    /**
     * Delegates
     */
    @Override
    public void dispose() {
        delegate.dispose();
    }

    /**
     * Delegates
     */
    @Override
    public boolean isLabelProperty(Object element, String property) {
        return delegate.isLabelProperty(element, property);
    }

    /**
     * Delegates
     */
    @Override
    public void removeListener(ILabelProviderListener listener) {
        delegate.removeListener(listener);
    }

    public MetricsScope getMetricsScope() {
        return settings.getMetricsScope();
    }
}
