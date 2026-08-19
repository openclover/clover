package org.openclover.core.reporters.pdf;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.cfg.Percentage;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.Column;
import org.openclover.core.reporters.ColumnFormat;
import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.util.HistoricalReportDescriptor;
import org.openclover.core.reporters.util.MetricsDiffSummary;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.util.Formatting;

import java.awt.Color;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The tables the coverage numbers themselves are rendered in: the per-package coverage grid, and
 * the "top movers" and "classes added" tables of the historical report.
 *
 * <p>Reached through {@link RenderingSupport}, which is the single entry point the reporters use.
 */
final class CoverageTables {

    static PdfTable createCoverageDataTable(final CloverReportConfig cfg, final String col0Title,
                                                   final List<? extends HasMetrics> items,
                                                   final PDFColours colours) {
        final PdfTable stats = createCoverageDataHeader(cfg, col0Title, colours);

        // the style every data row is drawn in; the few cells that deviate say so themselves
        stats.getDefaultStyle()
                .setBorderColour(colours.COL_TABLE_BORDER)
                .setBorders(PdfBorder.BOX)
                .setBackgroundColour(Color.white)
                .setHorizontalAlignment(PdfAlign.Horizontal.LEFT)
                .setVerticalAlignment(PdfAlign.Vertical.MIDDLE);

        for (HasMetrics item : items) {
            final BlockMetrics met = item.getMetrics();

            if (!cfg.getFormat().getShowEmpty() && met.getNumElements() == 0) {
                continue;
            }

            // project or package name - always add
            String name = item.getName();
            if (name == null) {
                name = "Project"; //##HACK !!!!!
            }
            stats.addCell(PdfText.of(name, RenderingSupport.TEXT_10));

            if (!cfg.isColumnsSet()) {
                // use standard set of columns
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredBranches()), RenderingSupport.TEXT_10), RenderingSupport.CENTERED);
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredStatements()), RenderingSupport.TEXT_10), RenderingSupport.CENTERED);
                stats.addCell(PdfText.of(
                        Formatting.getPercentStr(((ClassMetrics) met).getPcCoveredMethods()), RenderingSupport.TEXT_10), RenderingSupport.CENTERED);
                // the total and its bar share a cell border, so the two are ruled top and bottom
                // only and the bar closes the row off on the right
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredElements()), RenderingSupport.BOLD_10),
                        style -> style.setHorizontalAlignment(PdfAlign.Horizontal.CENTER)
                                .setBorders(PdfBorder.TOP, PdfBorder.BOTTOM));
                stats.addCell(createPCBar(met.getPcCoveredElements(), RenderingSupport.BAR_HEIGHT, colours),
                        style -> style.setBorders(PdfBorder.TOP, PdfBorder.BOTTOM, PdfBorder.RIGHT));
            } else {
                // use user-defined set of columns
                for (Column column : cfg.getColumns().getPkgColumns()) {
                    // render as bar, percentage or a raw value
                    if (column.getFormat() instanceof ColumnFormat.BarGraphColumnFormat) {
                        stats.addCell(createPCBar(fetchPercentageValue(column, met), RenderingSupport.BAR_HEIGHT, colours));
                    } else {
                        stats.addCell(PdfText.of(renderValue(column, met), RenderingSupport.TEXT_10));
                    }
                }
            }
        }
        return stats;
    }

    private static String renderValue(Column column, BlockMetrics met) {
        String value;
        try {
            column.init(met);
            value = column.render();
            column.reset();
        } catch (CloverException ex) {
            value = "n/a";
        }
        return value;
    }

    /**
     * Returns percentage as a range [0.0 ... 1.0]
     * @param column metric column
     * @param metrics    metric data
     * @return double [0.0 ... 1.0] or -1.0 in case of error
     */
    private static double fetchPercentageValue(Column column, BlockMetrics metrics) {
        double value;
        try {
            column.init(metrics);
            value = column.getNumber().doubleValue() / 100.0;
            column.reset();
        } catch (CloverException ex) {
            value = -1.0;
        }
        return value;
    }

    private static PdfTable createCoverageDataHeader(final CloverReportConfig cfg, final String col0Title,
                                                     final PDFColours colours) {
        final int numColumns = !cfg.isColumnsSet() ? 6 : 1 + cfg.getColumns().getPkgColumns().size();
        final PdfTable header = new PdfTable(numColumns);
        header.setWidthPercentage(100.0);
        // the shading applies to the header row only, so it is asked for per cell; the data rows
        // added afterwards set their own default style
        header.getDefaultStyle()
                .setBorderColour(colours.COL_TABLE_BORDER)
                .setBackgroundColour(colours.COL_HEADER_BG);

        if (!cfg.isColumnsSet()) {
            // use standard set of columns: package name, Branch, Stmt, Method, Total
            header.setWidths(new int[]{50, 10, 10, 10, 7, 13});
            header.addCell(PdfText.of(col0Title, RenderingSupport.BOLD_10));
            header.addCell(PdfText.of("Branch", RenderingSupport.BOLD_10), RenderingSupport.CENTERED);
            header.addCell(PdfText.of("Stmt", RenderingSupport.BOLD_10), RenderingSupport.CENTERED);
            header.addCell(PdfText.of("Method", RenderingSupport.BOLD_10), RenderingSupport.CENTERED);
            // "Total" heads both the percentage and the bar beside it
            header.addCell(PdfText.of("Total", RenderingSupport.BOLD_10), RenderingSupport.FULL_WIDTH);
        } else {
            header.setWidths(calculateEqualColumnWidths(numColumns));

            // use user-defined set of columns + one for a package name
            header.addCell(PdfText.of(col0Title, RenderingSupport.BOLD_10));
            cfg.getColumns().getPkgColumns()
                    .forEach(column -> header.addCell(PdfText.of(column.getName(), RenderingSupport.BOLD_10)));
        }

        return header;
    }

    /**
     * @return column proportions where the first column takes half the table and the remaining
     *         columns share the other half equally; computed in {@code double}, so that neither a
     *         single column nor a great many of them collapse the widths to zero
     */
    private static double[] calculateEqualColumnWidths(int numColumns) {
        final double first = 50.0;
        final double rest = numColumns > 1 ? first / (numColumns - 1) : 0.0;
        return IntStream.range(0, numColumns)
                .mapToDouble(i -> i == 0 ? first : rest)
                .toArray();
    }

    static PdfTable createMoversTable(HistoricalReportDescriptor.MoversDescriptor moversDesc,
                                             PDFColours colours) {
        final List<MetricsDiffSummary> gainers = moversDesc.getGainers();
        final List<MetricsDiffSummary> losers = moversDesc.getLosers();
        final String requestedPeriod = moversDesc.getRequestedInterval().toSensibleString();
        final String period = moversDesc.getActualInterval().toSensibleString();
        final Percentage threshold = moversDesc.getThreshold();
        final int range = moversDesc.getRange();

        final PdfTable movers = newMoversTable(colours);

        final PdfText title = PdfText.builder()
                .add("Top movers over the last " + requestedPeriod, RenderingSupport.BOLD_12)
                .add(" (Actual Interval: " + period + ", Range: " + range
                        + ", Threshold: +/-" + threshold + ")", RenderingSupport.ITALIC_8)
                .build();

        addMoversTitle(movers, title, colours);

        if (gainers.isEmpty() && losers.isEmpty()) {
            movers.addCell(PdfText.of("No changes in coverage are outside the specified threshold (+/-"
                    + threshold + ")", RenderingSupport.ITALIC_10), RenderingSupport.FULL_WIDTH);
            return movers;
        }

        if (gainers.isEmpty()) {
            // there are no gainers.
            movers.addCell(PdfText.of("No classes have gained coverage over threshold (+"
                    + threshold + ")", RenderingSupport.ITALIC_10), RenderingSupport.FULL_WIDTH);
        } else {
            for (MetricsDiffSummary diff : gainers) {
                movers.addCell(PdfText.of(diff.getName(), RenderingSupport.TEXT_8));
                movers.addCell(createPCDiffBar(diff, colours));
            }
        }
        if (losers.isEmpty()) {
            // there are no losers.
            movers.addCell(PdfText.of("No classes have lost coverage over threshold (-"
                    + threshold + ")", RenderingSupport.ITALIC_10), RenderingSupport.FULL_WIDTH);
        } else {
            // losers are laid out mirrored: the bar first, then the class name
            for (MetricsDiffSummary diff : losers) {
                movers.addCell(createPCDiffBar(diff, colours));
                movers.addCell(PdfText.of(diff.getName(), RenderingSupport.TEXT_8));
            }
        }
        return movers;
    }

    static PdfTable createAddedTable(HistoricalReportDescriptor.AddedDescriptor addedDesc,
                                            PDFColours colours) {
        final List<MetricsDiffSummary> gainers = addedDesc.getGainers();
        final String requestedPeriod = addedDesc.getRequestedInterval().toSensibleString();
        final String period = addedDesc.getActualInterval().toSensibleString();
        final int range = addedDesc.getRange();

        final PdfTable added = newMoversTable(colours);

        final PdfText title = PdfText.builder()
                .add("Classes added over the last " + requestedPeriod, RenderingSupport.BOLD_12)
                .add(" (Actual Interval: " + period + ", Range: " + range + ")", RenderingSupport.ITALIC_8)
                .build();

        addMoversTitle(added, title, colours);

        if (gainers.isEmpty()) {
            added.addCell(PdfText.of("No new classes", RenderingSupport.ITALIC_10), RenderingSupport.FULL_WIDTH);
            return added;
        }

        for (MetricsDiffSummary diff : gainers) {
            added.addCell(PdfText.of(diff.getName(), RenderingSupport.TEXT_8));
            added.addCell(createPCDiffBar(diff, colours));
        }
        return added;
    }

    /**
     * A two column table of class names and their coverage bars, in the style shared by the
     * "top movers" and "classes added" sections.
     */
    private static PdfTable newMoversTable(PDFColours colours) {
        final PdfTable movers = new PdfTable(2);
        movers.setWidthPercentage(100.0);
        movers.setWidths(new int[]{50, 50});
        // the body of the table; the title row above it is shaded and spans both columns
        movers.getDefaultStyle()
                .setBorderColour(colours.COL_TABLE_BORDER)
                .setBackgroundColour(Color.white);
        return movers;
    }

    private static void addMoversTitle(PdfTable movers, PdfText title, PDFColours colours) {
        movers.addCell(title, style -> style.setColspan(2)
                .setBackgroundColour(colours.COL_HEADER_BG));
    }

    private static CoverageDiffBarWidget createPCDiffBar(MetricsDiffSummary diff, PDFColours colours) {
        return new CoverageDiffBarWidget(diff.getPcDiff(), diff.getPc2float(), 8.0, colours);
    }

    private static CoverageBarWidget createPCBar(double pc, double height, PDFColours colours) {
        return new CoverageBarWidget(pc, height, RenderingSupport.BAR_PADDING_RATIO, colours);
    }
    private CoverageTables() {
    }
}
