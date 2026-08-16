package org.openclover.core.reporters.pdf.api;

/**
 * Anything that can be placed inside a {@link PdfCell}: text, a nested table or a self-drawing
 * {@link PdfWidget}.
 *
 * <p>Content measures itself through the {@link PdfLayout} it is handed, so adding a new kind of
 * content does not mean editing the layout engine.
 */
public interface PdfCellContent {

    /**
     * @param contentWidth width available once the cell's padding is taken off
     * @return this content measured against that width, ready to be drawn
     */
    PdfMeasuredContent measure(PdfLayout layout, PdfCellStyle style, double contentWidth);
}
