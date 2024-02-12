package org.openclover.eclipse.core.views;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.nodes.NodeHierarchyBuilder;

import java.util.List;

public abstract class ExplorerViewSettings implements IPersistable {
    public static final String HIERARCHY_STYLE = "hierarchy.style";

    protected int hierarchyStyle;
    protected ColumnCollectionSettings treeColumnSettings;

    public ExplorerViewSettings(IMemento memento) {
        hierarchyStyle = defaultHierarchyStyle();
        treeColumnSettings =
            new ColumnCollectionSettings(
                memento,
                allTreeColumns(),
                defaultTreeColumns(),
                defaultTreeColumnWidths());
        
        if (memento != null) {
            Integer hierarchyStyle = memento.getInteger(HIERARCHY_STYLE);
            if (hierarchyStyle != null) {
                this.hierarchyStyle = hierarchyStyle;
            }
        }
    }

    protected abstract List allTreeColumns();
    protected abstract List defaultTreeColumns();
    protected abstract List defaultTreeColumnWidths();

    public ColumnCollectionSettings getTreeColumnSettings() {
        return treeColumnSettings;
    }

    @Override
    public void saveState(IMemento memento) {
        memento.putInteger(HIERARCHY_STYLE, hierarchyStyle);
        treeColumnSettings.saveState(memento);
    }

    protected abstract int defaultHierarchyStyle();

    public void setHierarchyStyle(int style) {
        this.hierarchyStyle = style;
    }

    public int getHierarchyStyle() {
        return hierarchyStyle;
    }

    public abstract MetricsScope getMetricsScope();

    public NodeHierarchyBuilder nodeBuilderForStyle() {
        if (hierarchyStyle >= getHierarchyStyleStart() && hierarchyStyle <= getHierarchyStyleMax()) {
            return getNodeBuilders()[hierarchyStyle];
        } else {
            return getNodeBuilders()[defaultHierarchyStyle()];
        }
    }

    protected abstract int expandDepthForStyle();

    protected abstract NodeHierarchyBuilder[] getNodeBuilders();

    protected abstract int getHierarchyStyleMax();

    protected abstract int getHierarchyStyleStart();
}
