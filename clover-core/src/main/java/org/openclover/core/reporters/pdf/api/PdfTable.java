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
 * <p>Column widths are relative proportions of the table width; how wide the table itself is comes
 * from its {@link WidthMode}.
 */
public class PdfTable implements PdfBlock, PdfCellContent {

    /** Table width as a percentage of the available width, unless overridden. */
    public static final double DEFAULT_WIDTH_PERCENTAGE = 80.0;

    /**
     * How a table's width is derived from the width available to it.
     */
    public enum WidthMode {
        /**
         * No width was asked for: a top-level table takes {@link #DEFAULT_WIDTH_PERCENTAGE} of the
         * available width, matching the layout the reports were originally authored against, while
         * a nested table fills the cell it sits in.
         */
        AUTO,
        /** A percentage of the available width. */
        PERCENTAGE,
        /** An absolute number of points, independent of what is available. */
        ABSOLUTE
    }

    private final int numColumns;
    private final List<PdfCell> cells = new ArrayList<>();
    private final PdfCellStyle.Builder defaultStyle = PdfCellStyle.builder();

    private double[] relativeWidths;
    private WidthMode widthMode = WidthMode.AUTO;
    private double widthPercentage = DEFAULT_WIDTH_PERCENTAGE;
    private double totalWidth;

    /** Rows are grouped lazily and cached; adding a cell invalidates the grouping. */
    private List<List<PdfCell>> rows;

    public PdfTable(int numColumns) {
        if (numColumns < 1) {
            throw new IllegalArgumentException("a table needs at least one column");
        }
        this.numColumns = numColumns;
        this.relativeWidths = new double[numColumns];
        Arrays.fill(relativeWidths, 1.0);
    }

    public int getNumColumns() {
        return numColumns;
    }

    /**
     * @return the style template applied to each cell added afterwards
     */
    public PdfCellStyle.Builder getDefaultStyle() {
        return defaultStyle;
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

    /**
     * @return the column proportions; the array belongs to the table and must not be modified
     */
    public double[] getRelativeWidths() {
        return relativeWidths;
    }

    public WidthMode getWidthMode() {
        return widthMode;
    }

    public double getWidthPercentage() {
        return widthPercentage;
    }

    public PdfTable setWidthPercentage(double widthPercentage) {
        if (widthPercentage < 0.0) {
            throw new IllegalArgumentException("width percentage must not be negative: " + widthPercentage);
        }
        this.widthPercentage = widthPercentage;
        this.widthMode = WidthMode.PERCENTAGE;
        return this;
    }

    public double getTotalWidth() {
        return totalWidth;
    }

    /**
     * Pins the table to an absolute width, ignoring {@link #setWidthPercentage}.
     */
    public PdfTable setTotalWidth(double totalWidth) {
        if (totalWidth < 0.0) {
            throw new IllegalArgumentException("total width must not be negative: " + totalWidth);
        }
        this.totalWidth = totalWidth;
        this.widthMode = WidthMode.ABSOLUTE;
        return this;
    }

    /**
     * @param available width the table may occupy
     * @param nested    whether the table sits inside a cell
     * @return the width the table actually takes
     */
    public double resolveWidth(double available, boolean nested) {
        switch (widthMode) {
            case ABSOLUTE:
                return totalWidth;
            case PERCENTAGE:
                return available * widthPercentage / 100.0;
            default:
                return nested ? available : available * DEFAULT_WIDTH_PERCENTAGE / 100.0;
        }
    }

    /** Adds an empty cell, used as a spacer. */
    public PdfTable addCell() {
        return addCell(null);
    }

    public PdfTable addCell(PdfCellContent content) {
        cells.add(new PdfCell(defaultStyle.build(), content));
        rows = null;
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
        if (rows == null) {
            rows = groupIntoRows();
        }
        return rows;
    }

    private List<List<PdfCell>> groupIntoRows() {
        final List<List<PdfCell>> grouped = new ArrayList<>();
        List<PdfCell> row = new ArrayList<>();
        int used = 0;
        for (PdfCell cell : cells) {
            row.add(cell);
            used += Math.min(cell.getColspan(), numColumns);
            if (used >= numColumns) {
                grouped.add(Collections.unmodifiableList(row));
                row = new ArrayList<>();
                used = 0;
            }
        }
        if (!row.isEmpty()) {
            grouped.add(Collections.unmodifiableList(row));
        }
        return Collections.unmodifiableList(grouped);
    }

    @Override
    public double height(PdfLayout layout, PdfCellStyle style, double contentWidth) {
        return layout.nestedTableHeight(this, contentWidth);
    }

    @Override
    public void draw(PdfLayout layout, PdfCanvas canvas, PdfCellStyle style, PdfRect bounds) {
        layout.drawNestedTable(canvas, this, bounds.getX(), bounds.getTop(), bounds.getWidth());
    }
}
