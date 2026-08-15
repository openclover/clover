package org.openclover.core.reporters.pdf.api;

/**
 * Cell border edges, combined as a bit mask.
 */
public final class PdfBorder {

    public static final int NONE = 0;
    public static final int TOP = 1;
    public static final int BOTTOM = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int BOX = TOP | BOTTOM | LEFT | RIGHT;

    private PdfBorder() {
    }

    public static boolean has(int borders, int edge) {
        return (borders & edge) != 0;
    }
}
