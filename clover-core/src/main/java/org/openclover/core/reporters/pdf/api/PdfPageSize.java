package org.openclover.core.reporters.pdf.api;

/**
 * Page sizes supported by the PDF reporter, in PostScript points.
 */
public enum PdfPageSize {
    /** Rounded to whole points, the way the PDF reports have always been sized. */
    A4(595, 842),
    LETTER(612, 792);

    private final double width;
    private final double height;

    PdfPageSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
