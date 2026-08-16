package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfCell;
import org.openclover.core.reporters.pdf.api.PdfCellContent;
import org.openclover.core.reporters.pdf.api.PdfCellStyle;
import org.openclover.core.reporters.pdf.api.PdfLayout;
import org.openclover.core.reporters.pdf.api.PdfMeasuredContent;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfStroke;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;

/**
 * Measures and draws {@link PdfTable}s. This is the part iText provided and PDFBox does not: cells
 * are measured against their resolved column widths, rows take the height of their tallest cell,
 * and nested tables recurse.
 *
 * <p>Cell content measures itself through the {@link PdfLayout} this class implements, so no part
 * of the renderer needs to know which kinds of content exist. Measuring yields a
 * {@link PdfMeasuredContent} which is kept in the {@link TableLayout} and drawn as-is, so a nested
 * table is laid out exactly once however deeply it is nested.
 */
class TableRenderer implements PdfLayout {

    private final TextLayouter layouter;

    TableRenderer(TextLayouter layouter) {
        this.layouter = layouter;
    }

    /**
     * Measures a table against the width available to it.
     *
     * @param nested whether the table sits inside a cell
     */
    TableLayout layout(PdfTable table, double available, boolean nested) {
        final double tableWidth = table.resolveWidth(available, nested);
        final double[] columnWidths = columnWidths(table, tableWidth);

        final List<TableLayout.Row> rows = new ArrayList<>();
        for (List<PdfCell> cells : table.getRows()) {
            rows.add(measureRow(cells, columnWidths));
        }
        return new TableLayout(this, columnWidths, tableWidth, rows);
    }

    /**
     * Turns the relative column proportions into absolute widths.
     */
    private double[] columnWidths(PdfTable table, double tableWidth) {
        final double[] relative = table.getRelativeWidths().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        final double sum = DoubleStream.of(relative).sum();
        if (sum <= 0.0) {
            final double[] equal = new double[relative.length];
            Arrays.fill(equal, tableWidth / relative.length);
            return equal;
        }
        return DoubleStream.of(relative).map(width -> tableWidth * width / sum).toArray();
    }

    /**
     * Places and measures every cell of a row, the row taking the height of its tallest cell.
     */
    private TableLayout.Row measureRow(List<PdfCell> cells, double[] columnWidths) {
        final List<TableLayout.Cell> measured = new ArrayList<>(cells.size());
        double xOffset = 0.0;
        int column = 0;
        double height = 0.0;

        for (PdfCell cell : cells) {
            final double cellWidth = spannedWidth(columnWidths, column, cell.getColspan());
            final TableLayout.Cell measuredCell =
                    new TableLayout.Cell(cell, xOffset, cellWidth, measure(cell, cellWidth));
            measured.add(measuredCell);
            height = Math.max(height, measuredCell.getHeight());
            xOffset += cellWidth;
            column += cell.getColspan();
        }
        return new TableLayout.Row(measured, height);
    }

    private PdfMeasuredContent measure(PdfCell cell, double cellWidth) {
        final PdfCellContent content = cell.getContent();
        return content == null
                ? null
                : content.measure(this, cell.getStyle(), cell.contentWidth(cellWidth));
    }

    /**
     * Draws a whole table with its top-left corner at {@code (x, topY)}, measuring it first.
     */
    void drawTable(PdfCanvas canvas, PdfTable table, double x, double topY, double available) {
        draw(canvas, layout(table, available, false), x, topY);
    }

    /**
     * Draws an already-measured table with its top-left corner at {@code (x, topY)}.
     */
    void draw(PdfCanvas canvas, TableLayout layout, double x, double topY) {
        double cursorY = topY;
        for (TableLayout.Row row : layout.getRows()) {
            drawRow(canvas, row, x, cursorY);
            cursorY -= row.getHeight();
        }
    }

    void drawRow(PdfCanvas canvas, TableLayout.Row row, double x, double topY) {
        for (TableLayout.Cell cell : row.getCells()) {
            drawCell(canvas, cell, new PdfRect(x + cell.getXOffset(), topY - row.getHeight(),
                    cell.getWidth(), row.getHeight()));
        }
    }

    private void drawCell(PdfCanvas canvas, TableLayout.Cell measured, PdfRect bounds) {
        final PdfCellStyle style = measured.getCell().getStyle();
        if (style.getBackgroundColour() != null) {
            canvas.fillRect(bounds, style.getBackgroundColour());
        }
        drawBorders(canvas, style, bounds);

        final PdfMeasuredContent content = measured.getContent();
        if (content == null) {
            return;
        }
        content.draw(canvas, contentBounds(measured, bounds));
    }

    /**
     * Places the measured content inside its cell, honouring the style's vertical alignment.
     */
    private static PdfRect contentBounds(TableLayout.Cell measured, PdfRect bounds) {
        final PdfCell cell = measured.getCell();
        final PdfCellStyle style = cell.getStyle();
        final double contentHeight = measured.getContentHeight();
        final double slack = cell.contentHeight(bounds.getHeight()) - contentHeight;
        final double paddedTop = bounds.getTop() - style.getPaddingTop();

        final double top;
        switch (style.getVerticalAlignment()) {
            case MIDDLE:
                top = paddedTop - slack / 2.0;
                break;
            case BOTTOM:
                top = paddedTop - slack;
                break;
            default:
                top = paddedTop;
                break;
        }
        return new PdfRect(bounds.getX() + style.getPaddingLeft(), top - contentHeight,
                cell.contentWidth(bounds.getWidth()), contentHeight);
    }

    private void drawBorders(PdfCanvas canvas, PdfCellStyle style, PdfRect bounds) {
        final Set<PdfBorder> borders = style.getBorders();
        if (borders.isEmpty()) {
            return;
        }
        canvas.setLineWidth(PdfStroke.THIN_BORDER_WIDTH);
        if (borders.containsAll(PdfBorder.BOX)) {
            canvas.strokeRect(bounds, style.getBorderColour());
            return;
        }
        if (borders.contains(PdfBorder.TOP)) {
            canvas.drawLine(bounds.getX(), bounds.getTop(), bounds.getRight(), bounds.getTop(),
                    style.getBorderColour());
        }
        if (borders.contains(PdfBorder.BOTTOM)) {
            canvas.drawLine(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getY(),
                    style.getBorderColour());
        }
        if (borders.contains(PdfBorder.LEFT)) {
            canvas.drawLine(bounds.getX(), bounds.getY(), bounds.getX(), bounds.getTop(),
                    style.getBorderColour());
        }
        if (borders.contains(PdfBorder.RIGHT)) {
            canvas.drawLine(bounds.getRight(), bounds.getY(), bounds.getRight(), bounds.getTop(),
                    style.getBorderColour());
        }
    }

    private static double spannedWidth(double[] columnWidths, int firstColumn, int colspan) {
        double width = 0.0;
        for (int i = firstColumn; i < Math.min(firstColumn + colspan, columnWidths.length); i++) {
            width += columnWidths[i];
        }
        return width;
    }

    @Override
    public PdfMeasuredContent measureText(PdfText text, double width, PdfAlign.Horizontal alignment,
                                          double fixedLeading, double multipliedLeading) {
        return new MeasuredText(text, width, alignment, fixedLeading, multipliedLeading);
    }

    @Override
    public PdfMeasuredContent measureTable(PdfTable table, double availableWidth) {
        return layout(table, availableWidth, true);
    }

    /**
     * Text wrapped to a width, which is what fixes its height.
     */
    private class MeasuredText implements PdfMeasuredContent {

        private final PdfText text;
        private final PdfAlign.Horizontal alignment;
        private final double fixedLeading;
        private final double multipliedLeading;
        private final double height;

        MeasuredText(PdfText text, double width, PdfAlign.Horizontal alignment,
                     double fixedLeading, double multipliedLeading) {
            this.text = text;
            this.alignment = alignment;
            this.fixedLeading = fixedLeading;
            this.multipliedLeading = multipliedLeading;
            this.height = layouter.totalHeight(
                    layouter.layout(text, width), fixedLeading, multipliedLeading);
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public void draw(PdfCanvas canvas, PdfRect bounds) {
            canvas.drawText(text, bounds, alignment, PdfAlign.Vertical.TOP,
                    fixedLeading, multipliedLeading);
        }
    }
}
