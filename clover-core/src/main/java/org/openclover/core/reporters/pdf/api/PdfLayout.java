package org.openclover.core.reporters.pdf.api;

/**
 * The measuring services a {@link PdfCellContent} needs to place itself, provided by the layout
 * engine. Content asks the layout to measure it and hands back the result, rather than the engine
 * asking the content what type it is.
 */
public interface PdfLayout {

    /**
     * Wraps text to {@code width} and keeps the wrapping, so it is not recomputed when drawn.
     *
     * @param fixedLeading      line spacing is {@code fixed + multiplied * fontSize}
     * @param multipliedLeading see {@code fixedLeading}
     */
    PdfMeasuredContent measureText(PdfText text, double width, PdfAlign.Horizontal alignment,
                                   double fixedLeading, double multipliedLeading);

    /**
     * Lays out a table nested inside a cell of the given width, and keeps the layout.
     */
    PdfMeasuredContent measureTable(PdfTable table, double availableWidth);
}
