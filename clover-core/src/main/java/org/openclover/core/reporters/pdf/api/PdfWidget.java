package org.openclover.core.reporters.pdf.api;

/**
 * Self-drawing cell content, used for the coverage bars and the historical charts. The layout
 * engine asks for the height the widget wants, then hands it the rectangle it ended up with.
 */
public interface PdfWidget extends PdfCellContent {

    /**
     * @return height in points this widget needs, excluding cell padding
     */
    float preferredHeight();

    void draw(PdfCanvas canvas, PdfRect bounds);
}
