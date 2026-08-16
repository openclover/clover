package org.openclover.core.reporters.pdf.api;

/**
 * The measuring and drawing services a {@link PdfCellContent} needs to place itself, provided by
 * the layout engine. Content asks the layout how tall it would be and then tells it to draw,
 * rather than the engine asking the content what type it is.
 */
public interface PdfLayout {

    /**
     * @return the height the text occupies once wrapped to {@code width}, at the given leading
     */
    double textHeight(PdfText text, double width, double fixedLeading, double multipliedLeading);

    /**
     * Draws wrapped text filling {@code bounds} from its top edge downwards.
     */
    void drawText(PdfCanvas canvas, PdfText text, PdfRect bounds,
                  PdfAlign.Horizontal alignment, double fixedLeading, double multipliedLeading);

    /**
     * @return the height a table nested inside a cell of the given width occupies
     */
    double nestedTableHeight(PdfTable table, double availableWidth);

    /**
     * Draws a table nested inside a cell, with its first row's top edge at {@code topY}.
     */
    void drawNestedTable(PdfCanvas canvas, PdfTable table, double x, double topY, double availableWidth);
}
