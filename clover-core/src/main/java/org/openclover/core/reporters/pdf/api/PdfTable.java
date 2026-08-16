package org.openclover.core.reporters.pdf.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * A grid of {@link PdfCell}s. Cells are appended left to right and wrap to the next row once
 * their column spans fill the column count.
 *
 * <p>Column widths are relative proportions of the table width; how wide the table itself is comes
 * from its {@link WidthMode}.
 */
public class PdfTable implements PdfCellContent {

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
    private final PdfCellStyleBuilder defaultStyle = PdfCellStyle.builder();

    private List<Double> relativeWidths;
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
        // equal proportions until told otherwise
        this.relativeWidths = Collections.nCopies(numColumns, 1.0);
    }

    public int getNumColumns() {
        return numColumns;
    }

    /**
     * The style every cell of this table is given, meant to be configured once when the table is
     * created. A cell that has to deviate passes a customiser to
     * {@link #addCell(PdfCellContent, Consumer)} instead, which applies it to a copy and so leaves
     * this template — and every other cell — alone.
     *
     * @return the style template applied to each cell added afterward
     */
    public PdfCellStyleBuilder getDefaultStyle() {
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
        this.relativeWidths = Collections.unmodifiableList(
                DoubleStream.of(widths).boxed().collect(Collectors.toList()));
        return this;
    }

    /**
     * @return the column proportions, relative to their own sum
     */
    public List<Double> getRelativeWidths() {
        return relativeWidths;
    }

    public WidthMode getWidthMode() {
        return widthMode;
    }

    public double getWidthPercentage() {
        return widthPercentage;
    }

    /**
     * @throws IllegalArgumentException if the percentage is not positive; a table of no width
     *                                  wraps every word onto its own line and draws it into a
     *                                  column nothing is visible in
     */
    public PdfTable setWidthPercentage(double widthPercentage) {
        if (widthPercentage <= 0.0) {
            throw new IllegalArgumentException("width percentage must be positive: " + widthPercentage);
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
     *
     * @throws IllegalArgumentException if the width is not positive, for the reason given on
     *                                  {@link #setWidthPercentage}
     */
    public PdfTable setTotalWidth(double totalWidth) {
        if (totalWidth <= 0.0) {
            throw new IllegalArgumentException("total width must be positive: " + totalWidth);
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
        return addCell((PdfCellContent) null);
    }

    /** Adds a cell in the table's default style. */
    public PdfTable addCell(PdfCellContent content) {
        return addCell(new PdfCell(defaultStyle.build(), content));
    }

    /**
     * Adds a cell whose style deviates from the table's default. The customiser is applied to a
     * copy of the default style, so the deviation lasts for this one cell.
     *
     * @param customiser applied to a copy of {@link #getDefaultStyle()}
     */
    public PdfTable addCell(PdfCellContent content, Consumer<PdfCellStyleBuilder> customiser) {
        final PdfCellStyleBuilder style = defaultStyle.build().toBuilder();
        customiser.accept(style);
        return addCell(new PdfCell(style.build(), content));
    }

    /**
     * Adds an empty cell whose style deviates from the table's default. Named apart from
     * {@link #addCell(PdfCellContent, Consumer)} because both {@link Consumer} and
     * {@link PdfCellContent} have a single method, which makes a one-argument overload ambiguous
     * for a lambda.
     */
    public PdfTable addEmptyCell(Consumer<PdfCellStyleBuilder> customiser) {
        return addCell(null, customiser);
    }

    private PdfTable addCell(PdfCell cell) {
        cells.add(cell);
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
    public PdfMeasuredContent measure(PdfLayout layout, PdfCellStyle style, double contentWidth) {
        return layout.measureTable(this, contentWidth);
    }
}
