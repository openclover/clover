package com.atlassian.clover.eclipse.core.ui;

import org.eclipse.swt.layout.GridLayout;

/**
 * GLH - Grid Layout Helper - utility class for {@link org.eclipse.swt.layout.GridLayout}
 */
public class GLH {
    final GridLayout gridLayout;

    public GLH(int numColumns, boolean makeColumnsEqualWidth) {
        gridLayout = new GridLayout(numColumns, makeColumnsEqualWidth);
    }

    public GLH() {
        gridLayout = new GridLayout();
    }

    public GLH marginWidth(int witdh) {
        gridLayout.marginWidth = witdh;
        return this;
    }

    public GLH marginHeight(int height) {
        gridLayout.marginHeight = height;
        return this;
    }

    public GLH numColumns(int i) {
        gridLayout.numColumns = i;
        return this;
    }

    public GLH standardiseMargin() {
        gridLayout.marginHeight = 10;
        gridLayout.marginWidth = 10;
        return this;
    }

    public GLH verticalSpacing(int i) {
        gridLayout.verticalSpacing = i;
        return this;
    }

    public GridLayout getGridLayout() {
        return gridLayout;
    }
}

