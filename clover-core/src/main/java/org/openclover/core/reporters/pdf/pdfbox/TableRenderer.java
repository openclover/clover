package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfCell;
import org.openclover.core.reporters.pdf.api.PdfCellContent;
import org.openclover.core.reporters.pdf.api.PdfCellStyle;
import org.openclover.core.reporters.pdf.api.PdfLayout;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Measures and draws {@link PdfTable}s. This is the part iText provided and PDFBox does not: cells
 * are measured against their resolved column widths, rows take the height of their tallest cell,
 * and nested tables recurse.
 *
 * <p>Cell content measures and draws itself through the {@link PdfLayout} this class implements,
 * so no part of the renderer needs to know which kinds of content exist.
 */
class TableRenderer implements PdfLayout {

    private static final double BORDER_LINE_WIDTH = 0.5;

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
        final double[] columnWidths = columnWidths(table, table.resolveWidth(available, nested));
        final List<TableLayout.Row> rows = new ArrayList<>();
        for (List<PdfCell> cells : table.getRows()) {
            rows.add(new TableLayout.Row(cells, rowHeight(cells, columnWidths)));
        }
        return new TableLayout(columnWidths, rows);
    }

    /**
     * Turns the relative column proportions into absolute widths.
     */
    private double[] columnWidths(PdfTable table, double tableWidth) {
        final double[] relative = table.getRelativeWidths();
        final double sum = DoubleStream.of(relative).sum();
        if (sum <= 0.0) {
            final double[] equal = new double[relative.length];
            Arrays.fill(equal, tableWidth / relative.length);
            return equal;
        }
        return DoubleStream.of(relative).map(width -> tableWidth * width / sum).toArray();
    }

    private double rowHeight(List<PdfCell> row, double[] columnWidths) {
        double height = 0.0;
        int column = 0;
        for (PdfCell cell : row) {
            final double cellWidth = spannedWidth(columnWidths, column, cell.getColspan());
            height = Math.max(height, cellHeight(cell, cellWidth));
            column += cell.getColspan();
        }
        return height;
    }

    private double cellHeight(PdfCell cell, double cellWidth) {
        final double contentHeight = contentHeight(cell, cell.contentWidth(cellWidth));
        return Math.max(cell.getStyle().getMinimumHeight(),
                contentHeight + cell.getStyle().getPaddingTop() + cell.getStyle().getPaddingBottom());
    }

    private double contentHeight(PdfCell cell, double contentWidth) {
        final PdfCellContent content = cell.getContent();
        return content == null ? 0.0 : content.height(this, cell.getStyle(), contentWidth);
    }

    /**
     * Draws a whole table with its top-left corner at {@code (x, topY)}.
     */
    void drawTable(PdfCanvas canvas, PdfTable table, double x, double topY, double available) {
        drawTable(canvas, table, x, topY, available, false);
    }

    private void drawTable(PdfCanvas canvas, PdfTable table, double x, double topY,
                           double available, boolean nested) {
        final TableLayout layout = layout(table, available, nested);
        double cursorY = topY;
        for (TableLayout.Row row : layout.getRows()) {
            drawRow(canvas, row, layout.getColumnWidths(), x, cursorY);
            cursorY -= row.getHeight();
        }
    }

    void drawRow(PdfCanvas canvas, TableLayout.Row row, double[] columnWidths, double x, double topY) {
        double cellX = x;
        int column = 0;
        for (PdfCell cell : row.getCells()) {
            final double cellWidth = spannedWidth(columnWidths, column, cell.getColspan());
            drawCell(canvas, cell, new PdfRect(cellX, topY - row.getHeight(), cellWidth, row.getHeight()));
            cellX += cellWidth;
            column += cell.getColspan();
        }
    }

    private void drawCell(PdfCanvas canvas, PdfCell cell, PdfRect bounds) {
        final PdfCellStyle style = cell.getStyle();
        if (style.getBackgroundColour() != null) {
            canvas.fillRect(bounds, style.getBackgroundColour());
        }
        drawBorders(canvas, style, bounds);

        final PdfCellContent content = cell.getContent();
        if (content == null) {
            return;
        }

        final double contentWidth = cell.contentWidth(bounds.getWidth());
        final double contentX = bounds.getX() + style.getPaddingLeft();
        final double paddedTop = bounds.getTop() - style.getPaddingTop();
        final double paddedHeight = cell.contentHeight(bounds.getHeight());
        final double contentHeight = contentHeight(cell, contentWidth);
        final double top = style.getVerticalAlignment() == PdfAlign.Vertical.MIDDLE
                ? paddedTop - (paddedHeight - contentHeight) / 2.0
                : paddedTop;

        content.draw(this, canvas,
                style, new PdfRect(contentX, top - contentHeight, contentWidth, contentHeight));
    }

    private void drawBorders(PdfCanvas canvas, PdfCellStyle style, PdfRect bounds) {
        final Set<PdfBorder> borders = style.getBorders();
        if (borders.isEmpty()) {
            return;
        }
        canvas.setLineWidth(BORDER_LINE_WIDTH);
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
        return IntStream.range(firstColumn, Math.min(firstColumn + colspan, columnWidths.length))
                .mapToDouble(i -> columnWidths[i])
                .sum();
    }

    @Override
    public double textHeight(PdfText text, double width, double fixedLeading, double multipliedLeading) {
        return layouter.totalHeight(layouter.layout(text, width), fixedLeading, multipliedLeading);
    }

    @Override
    public void drawText(PdfCanvas canvas, PdfText text, PdfRect bounds,
                         PdfAlign.Horizontal alignment, double fixedLeading, double multipliedLeading) {
        canvas.drawText(text, bounds, alignment, PdfAlign.Vertical.TOP, fixedLeading, multipliedLeading);
    }

    @Override
    public double nestedTableHeight(PdfTable table, double availableWidth) {
        return layout(table, availableWidth, true).getHeight();
    }

    @Override
    public void drawNestedTable(PdfCanvas canvas, PdfTable table, double x, double topY,
                                double availableWidth) {
        drawTable(canvas, table, x, topY, availableWidth, true);
    }
}
