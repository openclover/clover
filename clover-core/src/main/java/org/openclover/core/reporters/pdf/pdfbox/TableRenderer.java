package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfCell;
import org.openclover.core.reporters.pdf.api.PdfCellContent;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfWidget;

import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Measures and draws {@link PdfTable}s. This is the part iText provided and PDFBox does not: cells
 * are measured against their resolved column widths, rows take the height of their tallest cell,
 * and nested tables recurse.
 */
class TableRenderer {

    private static final double BORDER_LINE_WIDTH = 0.5;

    private final FontRegistry fonts;
    private final TextLayouter layouter;

    TableRenderer(FontRegistry fonts) {
        this.fonts = fonts;
        this.layouter = new TextLayouter(fonts);
    }

    /**
     * @param available width the table may occupy
     * @return the width the table actually takes
     */
    double tableWidth(PdfTable table, double available) {
        return tableWidth(table, available, false);
    }

    /**
     * @param nested whether the table sits inside a cell; a nested table with no width of its own
     *               fills that cell, which is how the reports have always been laid out
     */
    double tableWidth(PdfTable table, double available, boolean nested) {
        if (table.hasTotalWidth()) {
            return table.getTotalWidth();
        }
        if (nested && !table.hasExplicitWidth()) {
            return available;
        }
        return available * table.getWidthPercentage() / 100;
    }

    /**
     * Turns the relative column proportions into absolute widths.
     */
    double[] columnWidths(PdfTable table, double tableWidth) {
        final double[] relative = table.getRelativeWidths();
        final double sum = DoubleStream.of(relative).sum();
        if (sum <= 0) {
            final double[] equal = new double[relative.length];
            Arrays.fill(equal, tableWidth / relative.length);
            return equal;
        }
        return DoubleStream.of(relative).map(width -> tableWidth * width / sum).toArray();
    }

    double rowHeight(List<PdfCell> row, double[] columnWidths) {
        double height = 0;
        int column = 0;
        for (PdfCell cell : row) {
            final double cellWidth = spannedWidth(columnWidths, column, cell.getColspan());
            height = Math.max(height, cellHeight(cell, cellWidth));
            column += cell.getColspan();
        }
        return height;
    }

    double tableHeight(PdfTable table, double available, boolean nested) {
        final double[] columnWidths = columnWidths(table, tableWidth(table, available, nested));
        return table.getRows().stream()
                .mapToDouble(row -> rowHeight(row, columnWidths))
                .sum();
    }

    private double cellHeight(PdfCell cell, double cellWidth) {
        final double contentWidth = Math.max(0,
                cellWidth - cell.getPaddingLeft() - cell.getPaddingRight());
        final double contentHeight = contentHeight(cell, contentWidth);
        return Math.max(cell.getMinimumHeight(),
                contentHeight + cell.getPaddingTop() + cell.getPaddingBottom());
    }

    private double contentHeight(PdfCell cell, double contentWidth) {
        final PdfCellContent content = cell.getContent();
        if (content == null) {
            return 0;
        }
        if (content instanceof PdfText) {
            return layouter.totalHeight(
                    layouter.layout((PdfText) content, contentWidth),
                    cell.getFixedLeading(), cell.getMultipliedLeading());
        }
        if (content instanceof PdfTable) {
            return tableHeight((PdfTable) content, contentWidth, true);
        }
        if (content instanceof PdfWidget) {
            return ((PdfWidget) content).preferredHeight();
        }
        return 0;
    }

    /**
     * Draws a whole table with its top-left corner at {@code (x, topY)}.
     */
    void drawTable(PdfBoxCanvas canvas, PdfTable table, double x, double topY, double available) {
        drawTable(canvas, table, x, topY, available, false);
    }

    private void drawTable(PdfBoxCanvas canvas, PdfTable table, double x, double topY,
                           double available, boolean nested) {
        final double[] columnWidths = columnWidths(table, tableWidth(table, available, nested));
        double cursorY = topY;
        for (List<PdfCell> row : table.getRows()) {
            final double height = rowHeight(row, columnWidths);
            drawRow(canvas, row, columnWidths, x, cursorY, height);
            cursorY -= height;
        }
    }

    void drawRow(PdfBoxCanvas canvas, List<PdfCell> row, double[] columnWidths,
                 double x, double topY, double rowHeight) {
        double cellX = x;
        int column = 0;
        for (PdfCell cell : row) {
            final double cellWidth = spannedWidth(columnWidths, column, cell.getColspan());
            drawCell(canvas, cell, new PdfRect(cellX, topY - rowHeight, cellWidth, rowHeight));
            cellX += cellWidth;
            column += cell.getColspan();
        }
    }

    private void drawCell(PdfBoxCanvas canvas, PdfCell cell, PdfRect bounds) {
        if (cell.getBackgroundColour() != null) {
            canvas.fillRect(bounds, cell.getBackgroundColour());
        }
        drawBorders(canvas, cell, bounds);

        final PdfCellContent content = cell.getContent();
        if (content == null) {
            return;
        }

        final double contentWidth = Math.max(0,
                bounds.getWidth() - cell.getPaddingLeft() - cell.getPaddingRight());
        final double contentX = bounds.getX() + cell.getPaddingLeft();
        final double paddedTop = bounds.getTop() - cell.getPaddingTop();
        final double paddedHeight = Math.max(0,
                bounds.getHeight() - cell.getPaddingTop() - cell.getPaddingBottom());
        final double contentHeight = contentHeight(cell, contentWidth);
        final double top = cell.getVerticalAlignment() == PdfAlign.Vertical.MIDDLE
                ? paddedTop - (paddedHeight - contentHeight) / 2
                : paddedTop;

        if (content instanceof PdfText) {
            drawText(canvas, cell, (PdfText) content, contentX, top, contentWidth);
        } else if (content instanceof PdfTable) {
            drawTable(canvas, (PdfTable) content, contentX, top, contentWidth, true);
        } else if (content instanceof PdfWidget) {
            final PdfWidget widget = (PdfWidget) content;
            widget.draw(canvas, new PdfRect(contentX, top - contentHeight, contentWidth, contentHeight));
        }
    }

    private void drawText(PdfBoxCanvas canvas, PdfCell cell, PdfText text,
                          double x, double top, double width) {
        final List<TextLayouter.Line> lines = layouter.layout(text, width);
        double cursorY = top;
        for (TextLayouter.Line line : lines) {
            cursorY -= TextLayouter.leadingOf(line, cell.getFixedLeading(), cell.getMultipliedLeading());
            final double lineX;
            switch (cell.getHorizontalAlignment()) {
                case RIGHT:
                    lineX = x + width - line.width;
                    break;
                case CENTER:
                    lineX = x + (width - line.width) / 2;
                    break;
                default:
                    lineX = x;
            }
            canvas.drawLine(line, lineX, cursorY - descentOf(line));
        }
    }

    /**
     * Descent is negative, so subtracting it lifts the baseline off the bottom of the line box.
     */
    private double descentOf(TextLayouter.Line line) {
        return line.pieces.stream()
                .mapToDouble(piece -> fonts.descent(piece.font))
                .min()
                .orElse(0);
    }

    private void drawBorders(PdfBoxCanvas canvas, PdfCell cell, PdfRect bounds) {
        final int borders = cell.getBorders();
        if (PdfBorder.isNone(borders)) {
            return;
        }
        canvas.setLineWidth(BORDER_LINE_WIDTH);
        if (borders == PdfBorder.BOX) {
            canvas.strokeRect(bounds, cell.getBorderColour());
            return;
        }
        if (PdfBorder.has(borders, PdfBorder.TOP)) {
            canvas.drawLine(bounds.getX(), bounds.getTop(), bounds.getRight(), bounds.getTop(),
                    cell.getBorderColour());
        }
        if (PdfBorder.has(borders, PdfBorder.BOTTOM)) {
            canvas.drawLine(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getY(),
                    cell.getBorderColour());
        }
        if (PdfBorder.has(borders, PdfBorder.LEFT)) {
            canvas.drawLine(bounds.getX(), bounds.getY(), bounds.getX(), bounds.getTop(),
                    cell.getBorderColour());
        }
        if (PdfBorder.has(borders, PdfBorder.RIGHT)) {
            canvas.drawLine(bounds.getRight(), bounds.getY(), bounds.getRight(), bounds.getTop(),
                    cell.getBorderColour());
        }
    }

    private static double spannedWidth(double[] columnWidths, int firstColumn, int colspan) {
        return IntStream.range(firstColumn, Math.min(firstColumn + colspan, columnWidths.length))
                .mapToDouble(i -> columnWidths[i])
                .sum();
    }
}
