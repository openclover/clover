package org.openclover.eclipse.core.views.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.openclover.eclipse.core.views.ColumnCollectionSettings;
import org.openclover.eclipse.core.views.ColumnDefinition;

public abstract class BaseListeningRenderer implements ListeningRenderer {
    /** 4 pixes each side for margin */
    protected static final int MARGIN = 4;

    protected Composite rendered;
    //HACK: there's no way to get row height from the paint event callback
    protected int lastRowHeight;
    //HACK: there's no way to get row top-left X from the paint event callback
    protected int lastRowX;
    //HACK: there's no way to get row top-left Y from the paint event callback
    protected int lastRowY;
    protected ColumnDefinition column;
    protected ColumnCollectionSettings settings;

    protected Listener paintListener = this::paint;
    protected Listener eraseListener = this::erase;

    public BaseListeningRenderer(Composite rendered, ColumnCollectionSettings settings, ColumnDefinition column) {
        this.rendered = rendered;
        this.settings = settings;
        this.column = column;
    }

    protected abstract void paint(Event event);

    protected abstract void erase(Event event);

    @Override
    public void startListening(Composite composite) {
        composite.addListener(SWT.PaintItem, paintListener);
        composite.addListener(SWT.EraseItem, eraseListener);
    }

    @Override
    public void stopListening(Composite composite) {
        composite.removeListener(SWT.PaintItem, paintListener);
        composite.removeListener(SWT.EraseItem, eraseListener);
    }

    protected boolean forSelection(Event event) {
        final TreeItem[] selection = ((Tree)rendered).getSelection();
        return selection != null && selection.length == 1 && selection[0] == event.item;
    }

    protected int calcTargetColumnCurrentWidth(Event event) {
        return ((Tree)event.widget).getColumns()[event.index].getWidth();
    }

    protected boolean forThisColumn(Event event) {
        return event.index == settings.getVisibleColumnIndexFor(column);
    }
}
