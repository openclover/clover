package org.openclover.eclipse.core.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

public class SwtUtils {
    /** Duplicated here as SWT.HOT doesn't exist in Eclipse 3.2 so compilation against 3.2 libs fails */
    public static final int SWT_HOT = 32;
    
    public static Label createMultilineLabel(Composite parent, String txt, int widthHint) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(txt);
        GridData gridData = new GridData(GridData.FILL);
        gridData.widthHint = widthHint;
        label.setLayoutData(gridData);
        return label;
    }

    public static Link createMultilineLink(Composite parent, String txt, int widthHint) {
        Link link = new Link(parent, SWT.WRAP);
        link.setText(txt);
        GridData gridData = new GridData(GridData.FILL);
        gridData.widthHint = widthHint;
        link.setLayoutData(gridData);
        return link;
    }

    public static Control setHorizontalSpan(Control control, int span) {
        gridDataFor(control).horizontalSpan = span;
        return control;
    }
    public static Control setVerticalSpan(Control control, int span) {
        gridDataFor(control).verticalSpan = span;
        return control;
    }

    public static GridData gridDataFor(Control control) {
        if (control.getLayoutData() == null || !(control.getLayoutData() instanceof GridData)) {
            control.setLayoutData(new GridData());
        }

        return (GridData)control.getLayoutData();
    }

    public static void drawText(
        String text, GC gc, Color foreground,
        int rowX, int rowY, int columnWidth, int rowHeight, int margin, int alignment) {

        gc.setForeground(foreground);

        //Calculate extent offset and size to place the right-justified text
        Point textSize = gc.textExtent(text);

        if (alignment == SWT.RIGHT) {
            gc.drawText(text, rowX + columnWidth - margin - textSize.x, rowY + ((rowHeight - textSize.y) / 2), true);
        } else if (alignment == SWT.LEFT) {
            gc.drawText(text, rowX + margin, rowY + ((rowHeight - textSize.y) / 2), true);
        } else {
            gc.drawText(text, rowX + (columnWidth - textSize.x) / 2, rowY + ((rowHeight - textSize.y) / 2), true);
        }
    }

    public static void renderSelectionBackground(Event event) {
        GC gc = event.gc;
        Color oldBackground = gc.getBackground();
        gc.setBackground(event.display.getSystemColor(SWT.COLOR_LIST_SELECTION));
        gc.fillRectangle(event.x, event.y, event.width, event.height);
        gc.setBackground(oldBackground);
    }
}
