package org.openclover.core.reporters.pdf.api;

/**
 * Horizontal and vertical alignment of cell content.
 */
public final class PdfAlign {

    public enum Horizontal {
        LEFT, CENTER, RIGHT
    }

    public enum Vertical {
        TOP, MIDDLE, BOTTOM
    }

    private PdfAlign() {
    }
}
