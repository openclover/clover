package org.openclover.core.reporters.pdf.api;

/**
 * What a {@link PdfPageDecorator} is given for each page. Decorators run after the document body
 * has been laid out, so the total page count is already known.
 */
public interface PdfPageContext {

    /** 1-based page number. */
    int getPageNumber();

    int getTotalPages();

    double getPageWidth();

    double getPageHeight();

    /** @return the margins the document body is laid out with, so decorators can line up with it */
    PdfMargins getMargins();

    PdfCanvas getCanvas();

    /**
     * Renders a table at an absolute position, with {@code topY} being the top edge of its first
     * row. The table must have an absolute width set via {@link PdfTable#setTotalWidth}.
     */
    void drawTable(PdfTable table, double x, double topY);
}
