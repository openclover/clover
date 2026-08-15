package org.openclover.core.reporters.pdf.pdfbox;

import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfCell;
import org.openclover.core.reporters.pdf.api.PdfCellContent;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfWidget;

import java.util.List;

/**
 * Measures and draws {@link PdfTable}s. This is the part iText provided and PDFBox does not: cells
 * are measured against their resolved column widths, rows take the height of their tallest cell,
 * and nested tables recurse.
 */
class TableRenderer {

    private static final float BORDER_LINE_WIDTH = 0.5f;

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
    float tableWidth(PdfTable table, float available) {
        return tableWidth(table, available, false);
    }

    /**
     * @param nested whether the table sits inside a cell; a nested table with no width of its own
     *               fills that cell, which is how the reports have always been laid out
     */
    float tableWidth(PdfTable table, float available, boolean nested) {
        if (table.hasTotalWidth()) {
            return table.getTotalWidth();
        }
        if (nested && !table.hasExplicitWidth()) {
            return available;
        }
        return available * table.getWidthPercentage() / 100f;
    }

    /**
     * Turns the relative column proportions into absolute widths.
     */
    float[] columnWidths(PdfTable table, float tableWidth) {
        final float[] relative = table.getRelativeWidths();
        float sum = 0f;
        for (float width : relative) {
            sum += width;
        }
        final float[] absolute = new float[relative.length];
        if (sum <= 0f) {
            java.util.Arrays.fill(absolute, tableWidth / relative.length);
            return absolute;
        }
        for (int i = 0; i < relative.length; i++) {
            absolute[i] = tableWidth * relative[i] / sum;
        }
        return absolute;
    }

    float rowHeight(List<PdfCell> row, float[] columnWidths) {
        float height = 0f;
        int column = 0;
        for (PdfCell cell : row) {
            final float cellWidth = spannedWidth(columnWidths, column, cell.getColspan());
            height = Math.max(height, cellHeight(cell, cellWidth));
            column += cell.getColspan();
        }
        return height;
    }

    float tableHeight(PdfTable table, float available, boolean nested) {
        final float[] columnWidths = columnWidths(table, tableWidth(table, available, nested));
        float height = 0f;
        for (List<PdfCell> row : table.getRows()) {
            height += rowHeight(row, columnWidths);
        }
        return height;
    }

    private float cellHeight(PdfCell cell, float cellWidth) {
        final float contentWidth = Math.max(0f,
                cellWidth - cell.getPaddingLeft() - cell.getPaddingRight());
        final float contentHeight = contentHeight(cell, contentWidth);
        return Math.max(cell.getMinimumHeight(),
                contentHeight + cell.getPaddingTop() + cell.getPaddingBottom());
    }

    private float contentHeight(PdfCell cell, float contentWidth) {
        final PdfCellContent content = cell.getContent();
        if (content == null) {
            return 0f;
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
        return 0f;
    }

    /**
     * Draws a whole table with its top-left corner at {@code (x, topY)}.
     */
    void drawTable(PdfBoxCanvas canvas, PdfTable table, float x, float topY, float available) {
        drawTable(canvas, table, x, topY, available, false);
    }

    private void drawTable(PdfBoxCanvas canvas, PdfTable table, float x, float topY,
                           float available, boolean nested) {
        final float[] columnWidths = columnWidths(table, tableWidth(table, available, nested));
        float cursorY = topY;
        for (List<PdfCell> row : table.getRows()) {
            final float height = rowHeight(row, columnWidths);
            drawRow(canvas, row, columnWidths, x, cursorY, height);
            cursorY -= height;
        }
    }

    void drawRow(PdfBoxCanvas canvas, List<PdfCell> row, float[] columnWidths,
                 float x, float topY, float rowHeight) {
        float cellX = x;
        int column = 0;
        for (PdfCell cell : row) {
            final float cellWidth = spannedWidth(columnWidths, column, cell.getColspan());
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

        final float contentWidth = Math.max(0f,
                bounds.getWidth() - cell.getPaddingLeft() - cell.getPaddingRight());
        final float contentX = bounds.getX() + cell.getPaddingLeft();
        final float paddedTop = bounds.getTop() - cell.getPaddingTop();
        final float paddedHeight = Math.max(0f,
                bounds.getHeight() - cell.getPaddingTop() - cell.getPaddingBottom());
        final float contentHeight = contentHeight(cell, contentWidth);
        final float top = cell.getVerticalAlignment() == PdfAlign.Vertical.MIDDLE
                ? paddedTop - (paddedHeight - contentHeight) / 2f
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
                          float x, float top, float width) {
        final List<TextLayouter.Line> lines = layouter.layout(text, width);
        float cursorY = top;
        for (TextLayouter.Line line : lines) {
            cursorY -= TextLayouter.leadingOf(line, cell.getFixedLeading(), cell.getMultipliedLeading());
            final float lineX;
            switch (cell.getHorizontalAlignment()) {
                case RIGHT:
                    lineX = x + width - line.width;
                    break;
                case CENTER:
                    lineX = x + (width - line.width) / 2f;
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
    private float descentOf(TextLayouter.Line line) {
        float descent = 0f;
        for (TextLayouter.Piece piece : line.pieces) {
            descent = Math.min(descent, fonts.descent(piece.font));
        }
        return descent;
    }

    private void drawBorders(PdfBoxCanvas canvas, PdfCell cell, PdfRect bounds) {
        final int borders = cell.getBorders();
        if (borders == PdfBorder.NONE) {
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

    private static float spannedWidth(float[] columnWidths, int firstColumn, int colspan) {
        float width = 0f;
        for (int i = firstColumn; i < firstColumn + colspan && i < columnWidths.length; i++) {
            width += columnWidths[i];
        }
        return width;
    }
}
