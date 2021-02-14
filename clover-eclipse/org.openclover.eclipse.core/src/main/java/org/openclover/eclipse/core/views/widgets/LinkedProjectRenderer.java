package org.openclover.eclipse.core.views.widgets;

import org.openclover.eclipse.core.views.ColumnCollectionSettings;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TreeItem;

public class LinkedProjectRenderer extends SelectionAwareCellRenderer {
    public LinkedProjectRenderer(Composite rendered, ColumnCollectionSettings settings, ColumnDefinition column) {
        super(settings, column, rendered);
    }

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

    @Override
    protected void paint(Event event) {
        if (forThisColumn(event) && event.item.getData() instanceof IProject) {
            final TreeItem treeItem = (TreeItem)event.item;
            final Rectangle imageBounds = treeItem.getImageBounds(0);
            final Point textTopLeft = new Point(imageBounds.x + imageBounds.width + 5, imageBounds.y);
            final Point textSize = event.gc.textExtent(((IProject)event.item.getData()).getName());
            event.gc.drawLine(textTopLeft.x, textTopLeft.y + textSize.y + 1, textTopLeft.x + textSize.x, textTopLeft.y + textSize.y + 1);
        }
    }
}
