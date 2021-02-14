package org.openclover.eclipse.core.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.viewers.ILabelProvider;

import java.util.WeakHashMap;
import java.util.Comparator;

import org.openclover.eclipse.core.views.widgets.ListeningRenderer;
import org.openclover.eclipse.core.views.widgets.SelectionAwareCellRenderer;
import org.openclover.eclipse.core.projects.model.MetricsScope;

public abstract class ColumnDefinition {
    public static final int ANY_COLUMN = -1;
    public static final Comparator TITLE_COMPARATOR = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((ColumnDefinition)o1).getTitle().compareTo(((ColumnDefinition)o2).getTitle());
        }
    };

    private final String id;
    private final int lockedColumnIndex;
    private int alignment;
    private String title;
    private String abbreviatedTitle;
    private String tooltip;
    private WeakHashMap renderers;

    protected ColumnDefinition(String id, int pinndedIndex, int alignment, String title, String abbreviatedTitle, String tooltip) {
        this.id = id;
        this.lockedColumnIndex = pinndedIndex;
        this.alignment = (alignment == SWT.LEFT || alignment == SWT.CENTER || alignment == SWT.RIGHT) ? alignment : SWT.LEFT;
        this.title = title;
        this.abbreviatedTitle = abbreviatedTitle;
        this.tooltip = tooltip;
        this.renderers = new WeakHashMap();
    }

    public String getTitle() {
        return title;
    }

    public String getAbbreviatedTitle() {
        return abbreviatedTitle;
    }

    public String getId() {
        return id;
    }

    public int getAlignment() {
        return alignment;
    }

    public String getTooltip() {
        return tooltip;
    }

    public boolean isLocked() {
        return lockedColumnIndex != ANY_COLUMN;
    }

    public int getLockedColumnIndex() {
        return lockedColumnIndex;
    }

    public void bindRenderer(Composite composite, ExplorerViewSettings settings) {
        ListeningRenderer renderer = newRenderer(composite, settings);
        if (renderer != null) {
            renderer.startListening(composite);
            renderers.put(composite, renderer);
        }
    }

    public void unbindRenderer(Composite composite) {
        ListeningRenderer renderer = (ListeningRenderer)renderers.remove(composite);
        if (renderer != null) {
            renderer.stopListening(composite);
        }
    }

    public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
        return new SelectionAwareCellRenderer(settings.getTreeColumnSettings(), this, composite) { };
    }

    public abstract boolean isCustom();

    public abstract String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element);

    public Image getImage(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
        return null;
    }

    public abstract Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope);

    public boolean displaysSimpleLabel() {
        return false;
    }

    public boolean displaysImage() {
        return false;
    }
}
