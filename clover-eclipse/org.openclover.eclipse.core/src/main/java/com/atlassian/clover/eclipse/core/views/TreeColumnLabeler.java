package com.atlassian.clover.eclipse.core.views;

import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.graphics.GC;

public class TreeColumnLabeler {
    private static final float MARGIN_GUESTIMATE = 5.0f;

    public String columnTitleFor(TreeColumn column, ColumnDefinition columnDef) {
        final GC gc = new GC(column.getDisplay());
        try {
            final int titleLength = gc.textExtent(columnDef.getTitle()).x;
            final int elipsisLength = gc.textExtent("...").x;

            //If the column width is less than 100% of its estimated required width, switch to abbreviations
            //as elipses tend to clobber everything
            if (titleLength > 0
                && ((((float)column.getWidth()) / ((float)titleLength + (2 * MARGIN_GUESTIMATE))) < 1.0f)) {
                return columnDef.getAbbreviatedTitle();
            } else {
                return columnDef.getTitle();
            }
        } finally {
            gc.dispose();
        }
    }
}
