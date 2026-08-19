package org.openclover.core.reporters.pdf.api;

/**
 * A single table cell: an immutable pairing of a {@link PdfCellStyle} with the content drawn
 * inside it.
 */
public final class PdfCell {

    private final PdfCellStyle style;
    private final PdfCellContent content;

    /**
     * @param content what to draw inside the cell, or null for an empty spacer cell
     */
    public PdfCell(PdfCellStyle style, PdfCellContent content) {
        this.style = style;
        this.content = content;
    }

    public PdfCellStyle getStyle() {
        return style;
    }

    /** @return the cell's content, or null when the cell is empty */
    public PdfCellContent getContent() {
        return content;
    }

    public int getColspan() {
        return style.getColspan();
    }

    /**
     * @param cellWidth the cell's outer width
     * @return the width left for the content once horizontal padding is taken off, never negative
     */
    public double contentWidth(double cellWidth) {
        return Math.max(0.0, cellWidth - style.getPaddingLeft() - style.getPaddingRight());
    }

    /**
     * @param cellHeight the cell's outer height
     * @return the height left for the content once vertical padding is taken off, never negative
     */
    public double contentHeight(double cellHeight) {
        return Math.max(0.0, cellHeight - style.getPaddingTop() - style.getPaddingBottom());
    }
}
