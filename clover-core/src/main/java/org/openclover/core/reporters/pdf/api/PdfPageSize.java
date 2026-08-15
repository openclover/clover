package org.openclover.core.reporters.pdf.api;

/**
 * Page sizes supported by the PDF reporter, in PostScript points.
 */
public enum PdfPageSize {
    /** Rounded to whole points, the way the PDF reports have always been sized. */
    A4(595f, 842f),
    LETTER(612f, 792f);

    private final float width;
    private final float height;

    PdfPageSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
