package org.openclover.core.reporters.pdf.api;

/**
 * Anything that can be placed inside a {@link PdfCell}: text, a nested table or a self-drawing
 * {@link PdfWidget}.
 *
 * <p>Content measures and draws itself through the {@link PdfLayout} it is handed, so adding a new
 * kind of content does not mean editing the layout engine.
 */
public interface PdfCellContent {

    /**
     * @param contentWidth width available once the cell's padding is taken off
     * @return the height this content needs
     */
    double height(PdfLayout layout, PdfCellStyle style, double contentWidth);

    /**
     * @param bounds the rectangle the layout engine resolved for this content, padding excluded
     */
    void draw(PdfLayout layout, PdfCanvas canvas, PdfCellStyle style, PdfRect bounds);
}
