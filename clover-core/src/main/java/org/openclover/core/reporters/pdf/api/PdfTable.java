package org.openclover.core.reporters.pdf.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A grid of {@link PdfCell}s. Cells are appended left to right and wrap to the next row once
 * their colspans fill the column count.
 *
 * <p>Column widths are relative proportions of the table width; the table width itself is either
 * a percentage of the space available to it (the default, 80%, matching the layout the reports
 * were originally authored against) or an absolute number of points.
 */
public class PdfTable implements PdfBlock, PdfCellContent {

    /** Table width as a percentage of the available width, unless overridden. */
    public static final double DEFAULT_WIDTH_PERCENTAGE = 80;

    private final int numColumns;
    private final List<PdfCell> cells = new ArrayList<>();
    private final PdfCell defaultCell = new PdfCell();

    private double[] relativeWidths;
    private double widthPercentage = DEFAULT_WIDTH_PERCENTAGE;
    private double totalWidth = -1;
    private boolean widthSetExplicitly;

    public PdfTable(int numColumns) {
        if (numColumns < 1) {
            throw new IllegalArgumentException("a table needs at least one column");
        }
        this.numColumns = numColumns;
        this.relativeWidths = new double[numColumns];
        Arrays.fill(relativeWidths, 1);
    }

    public int getNumColumns() {
        return numColumns;
    }

    /**
     * @return the template whose state is copied into each cell added afterwards
     */
    public PdfCell getDefaultCell() {
        return defaultCell;
    }

    public PdfTable setWidths(int[] widths) {
        return setWidths(IntStream.of(widths).asDoubleStream().toArray());
    }

    public PdfTable setWidths(double[] widths) {
        if (widths.length != numColumns) {
            throw new IllegalArgumentException(
                    "expected " + numColumns + " column widths, got " + widths.length);
        }
        this.relativeWidths = widths.clone();
        return this;
    }

    public double[] getRelativeWidths() {
        return relativeWidths.clone();
    }

    public double getWidthPercentage() {
        return widthPercentage;
    }

    public PdfTable setWidthPercentage(double widthPercentage) {
        this.widthPercentage = widthPercentage;
        this.widthSetExplicitly = true;
        return this;
    }

    /**
     * Whether a width was asked for. A nested table that was not given one fills its cell rather
     * than falling back to {@link #DEFAULT_WIDTH_PERCENTAGE}.
     */
    public boolean hasExplicitWidth() {
        return widthSetExplicitly;
    }

    public double getTotalWidth() {
        return totalWidth;
    }

    /**
     * Pins the table to an absolute width, ignoring {@link #setWidthPercentage}.
     */
    public PdfTable setTotalWidth(double totalWidth) {
        this.totalWidth = totalWidth;
        this.widthSetExplicitly = true;
        return this;
    }

    public boolean hasTotalWidth() {
        return totalWidth >= 0;
    }

    /** Adds an empty cell, used as a spacer. */
    public PdfTable addCell() {
        return addCell((PdfCellContent) null);
    }

    public PdfTable addCell(PdfCellContent content) {
        cells.add(new PdfCell(defaultCell).setContent(content));
        return this;
    }

    public List<PdfCell> getCells() {
        return Collections.unmodifiableList(cells);
    }

    /**
     * Groups the cells added so far into rows. A trailing partial row is returned as-is; the
     * layout engine treats the missing columns as empty.
     */
    public List<List<PdfCell>> getRows() {
        final List<List<PdfCell>> rows = new ArrayList<>();
        List<PdfCell> row = new ArrayList<>();
        int used = 0;
        for (PdfCell cell : cells) {
            row.add(cell);
            used += Math.min(cell.getColspan(), numColumns);
            if (used >= numColumns) {
                rows.add(row);
                row = new ArrayList<>();
                used = 0;
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return rows;
    }
}
