package org.openclover.core.reporters.pdf.api;

/**
 * Cell content that has been measured against a width and is ready to be drawn at that width.
 *
 * <p>Measuring a table is what establishes its row heights, so the result of measuring is kept and
 * drawn rather than being thrown away and recomputed: without this a table nested {@code k} deep
 * would be laid out {@code 2^k} times.
 */
public interface PdfMeasuredContent {

    /** @return the height the measured content occupies at the width it was measured for */
    double getHeight();

    /**
     * @param bounds the rectangle the layout engine resolved, whose height is {@link #getHeight()}
     */
    void draw(PdfCanvas canvas, PdfRect bounds);
}
