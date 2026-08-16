package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfCell;

import java.util.Collections;
import java.util.List;

/**
 * A table measured against the width it was given: absolute column widths plus the height of every
 * row. Measuring is separated from drawing so that both the document flow, which has to decide
 * where a page breaks, and the renderer, which just paints, work from the same numbers.
 */
class TableLayout {

    /** One measured row. */
    static class Row {

        private final List<PdfCell> cells;
        private final double height;

        Row(List<PdfCell> cells, double height) {
            this.cells = cells;
            this.height = height;
        }

        List<PdfCell> getCells() {
            return cells;
        }

        double getHeight() {
            return height;
        }
    }

    private final double[] columnWidths;
    private final List<Row> rows;

    TableLayout(double[] columnWidths, List<Row> rows) {
        this.columnWidths = columnWidths;
        this.rows = Collections.unmodifiableList(rows);
    }

    /** @return the resolved column widths; the array belongs to the layout, do not modify it */
    double[] getColumnWidths() {
        return columnWidths;
    }

    List<Row> getRows() {
        return rows;
    }

    double getHeight() {
        return rows.stream().mapToDouble(Row::getHeight).sum();
    }
}
