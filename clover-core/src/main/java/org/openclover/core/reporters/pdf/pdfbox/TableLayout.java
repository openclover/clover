package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfCell;
import org.openclover.core.reporters.pdf.api.PdfMeasuredContent;
import org.openclover.core.reporters.pdf.api.PdfRect;

import java.util.Collections;
import java.util.List;

/**
 * A table measured against the width it was given: every cell placed and sized, and every row's
 * height established. Measuring is separated from drawing so that both the document flow, which
 * has to decide where a page breaks, and the renderer, which just paints, work from the same
 * numbers — and so that nothing is measured twice.
 */
class TableLayout implements PdfMeasuredContent {

    /** One measured cell: where it sits in its row, and its content already measured. */
    static class Cell {

        private final PdfCell cell;
        private final double xOffset;
        private final double width;
        private final PdfMeasuredContent content;

        /**
         * @param xOffset from the table's left edge
         * @param width   the cell's outer width, spanned columns included
         * @param content the measured content, or null for an empty cell
         */
        Cell(PdfCell cell, double xOffset, double width, PdfMeasuredContent content) {
            this.cell = cell;
            this.xOffset = xOffset;
            this.width = width;
            this.content = content;
        }

        PdfCell getCell() {
            return cell;
        }

        double getXOffset() {
            return xOffset;
        }

        double getWidth() {
            return width;
        }

        /** @return the measured content, or null when the cell is empty */
        PdfMeasuredContent getContent() {
            return content;
        }

        double getContentHeight() {
            return content == null ? 0.0 : content.getHeight();
        }

        /** @return the outer height this cell alone would need */
        double getHeight() {
            return Math.max(cell.getStyle().getMinimumHeight(),
                    getContentHeight() + cell.getStyle().getPaddingTop()
                            + cell.getStyle().getPaddingBottom());
        }
    }

    /** One measured row, as tall as its tallest cell. */
    static class Row {

        private final List<Cell> cells;
        private final double height;

        Row(List<Cell> cells, double height) {
            this.cells = Collections.unmodifiableList(cells);
            this.height = height;
        }

        List<Cell> getCells() {
            return cells;
        }

        double getHeight() {
            return height;
        }
    }

    private final TableRenderer renderer;
    private final double[] columnWidths;
    private final double width;
    private final List<Row> rows;

    TableLayout(TableRenderer renderer, double[] columnWidths, double width, List<Row> rows) {
        this.renderer = renderer;
        this.columnWidths = columnWidths;
        this.width = width;
        this.rows = Collections.unmodifiableList(rows);
    }

    /**
     * @return the resolved column widths; package-private and never handed outside this package,
     *         so the array is not copied
     */
    double[] getColumnWidths() {
        return columnWidths;
    }

    /** @return the width the table resolved to */
    double getWidth() {
        return width;
    }

    List<Row> getRows() {
        return rows;
    }

    @Override
    public double getHeight() {
        return rows.stream().mapToDouble(Row::getHeight).sum();
    }

    @Override
    public void draw(PdfCanvas canvas, PdfRect bounds) {
        renderer.draw(canvas, this, bounds.getX(), bounds.getTop());
    }
}
