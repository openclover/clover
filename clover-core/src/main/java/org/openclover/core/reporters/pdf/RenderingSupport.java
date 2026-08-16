package org.openclover.core.reporters.pdf;

import org.jfree.chart.JFreeChart;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.cfg.Percentage;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.registry.metrics.ProjectMetrics;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.Column;
import org.openclover.core.reporters.ColumnFormat;
import org.openclover.core.reporters.Historical;
import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfCellStyle;
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfFontStyle;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfTextBuilder;
import org.openclover.core.reporters.util.CloverChartFactory;
import org.openclover.core.reporters.util.HistoricalReportDescriptor;
import org.openclover.core.reporters.util.MetricsDiffSummary;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.util.Formatting;

import java.awt.Color;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Builds the tables the PDF reports are made of. One static factory per report element.
 */
public class RenderingSupport {

    private static final PdfFontSpec TEXT_8 = PdfFontSpec.sans(8);
    private static final PdfFontSpec ITALIC_8 = PdfFontSpec.sans(8, PdfFontStyle.ITALIC);
    private static final PdfFontSpec TEXT_10 = PdfFontSpec.sans(10);
    private static final PdfFontSpec BOLD_10 = PdfFontSpec.sans(10, PdfFontStyle.BOLD);
    private static final PdfFontSpec ITALIC_10 = PdfFontSpec.sans(10, PdfFontStyle.ITALIC);
    private static final PdfFontSpec BOLD_12 = PdfFontSpec.sans(12, PdfFontStyle.BOLD);
    private static final PdfFontSpec BOLD_14 = PdfFontSpec.sans(14, PdfFontStyle.BOLD);

    /** Fraction of a cell's width left blank on either side of a coverage bar. */
    private static final double BAR_PADDING_RATIO = 0.01;

    /** Height of the coverage bars, in points. */
    private static final double BAR_HEIGHT = 10.0;

    /** Style deviations used often enough to be worth naming. */
    private static final Consumer<PdfCellStyle.Builder> CENTERED =
            style -> style.setHorizontalAlignment(PdfAlign.Horizontal.CENTER);

    private static final Consumer<PdfCellStyle.Builder> RIGHT_ALIGNED =
            style -> style.setHorizontalAlignment(PdfAlign.Horizontal.RIGHT);

    /** A cell spanning both columns of a two column table. */
    private static final Consumer<PdfCellStyle.Builder> FULL_WIDTH = style -> style.setColspan(2);

    public static PdfTable getSpacerRow() {
        final PdfTable spacer = new PdfTable(1);
        spacer.getDefaultStyle().setBorders(PdfBorder.NONE);
        spacer.setWidthPercentage(100.0);
        spacer.addCell(PdfText.of(" ", TEXT_10));
        return spacer;
    }

    public static PdfTable createReportHeader(ProjectInfo hasMetrics, long ts, String title,
                                              String titleAnchor, PDFColours colours) {
        return createReportHeader((HasMetrics) hasMetrics, ts, title, titleAnchor, colours);
    }

    public static PdfTable createReportHeader(PackageInfo hasMetrics, long ts, String title,
                                              String titleAnchor, PDFColours colours) {
        return createReportHeader((HasMetrics) hasMetrics, ts, title, titleAnchor, colours);
    }

    public static PdfTable createCoverageDataTable(final CloverReportConfig cfg, final String col0Title,
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
            stats.addCell(PdfText.of(name, TEXT_10));

            if (!cfg.isColumnsSet()) {
                // use standard set of columns
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredBranches()), TEXT_10), CENTERED);
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredStatements()), TEXT_10), CENTERED);
                stats.addCell(PdfText.of(
                        Formatting.getPercentStr(((ClassMetrics) met).getPcCoveredMethods()), TEXT_10), CENTERED);
                // the total and its bar share a cell border, so the two are ruled top and bottom
                // only and the bar closes the row off on the right
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredElements()), BOLD_10),
                        style -> style.setHorizontalAlignment(PdfAlign.Horizontal.CENTER)
                                .setBorders(PdfBorder.TOP, PdfBorder.BOTTOM));
                stats.addCell(createPCBar(met.getPcCoveredElements(), BAR_HEIGHT, colours),
                        style -> style.setBorders(PdfBorder.TOP, PdfBorder.BOTTOM, PdfBorder.RIGHT));
            } else {
                // use user-defined set of columns
                for (Column column : cfg.getColumns().getPkgColumns()) {
                    // render as bar, percentage or a raw value
                    if (column.getFormat() instanceof ColumnFormat.BarGraphColumnFormat) {
                        stats.addCell(createPCBar(fetchPercentageValue(column, met), BAR_HEIGHT, colours));
                    } else {
                        stats.addCell(PdfText.of(renderValue(column, met), TEXT_10));
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

    public static PdfTable createChart(Historical.Chart chartCfg, Map<Long, ? extends HasMetrics> data,
                                       PDFColours colours) {
        final PdfTable coverage = new PdfTable(1);
        coverage.setWidthPercentage(100.0);
        coverage.getDefaultStyle()
                .setBorderColour(colours.COL_TABLE_BORDER)
                .setBackgroundColour(Color.white)
                .setPaddingLeft(2.0);

        // a shaded strip above the graph, matching the header of the tables around it
        coverage.addCell(PdfText.of(" ", BOLD_12),
                style -> style.setBackgroundColour(colours.COL_HEADER_BG));

        final JFreeChart graph = CloverChartFactory.createJFreeChart(chartCfg, data);
        coverage.addCell(new ChartWidget(graph, chartCfg.getHeight()));
        return coverage;
    }

    private static PdfTable createHeaderStats(PackageMetrics metrics, PDFColours colours) {
        final String level = metrics.getType();

        final PdfTable projStats = new PdfTable(5);
        projStats.setWidths(new int[]{30, 17, 18, 20, 15});
        projStats.getDefaultStyle()
                .setBorders(PdfBorder.NONE)
                .setHorizontalAlignment(PdfAlign.Horizontal.RIGHT);
        projStats.addCell(PdfText.of(level + " stats:", BOLD_10));
        projStats.addCell(PdfText.of("LOC:", BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getLineCount()), TEXT_10));
        projStats.addCell(PdfText.of("Methods:", BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNumMethods()), TEXT_10));
        projStats.addCell();
        projStats.addCell(PdfText.of("NCLOC:", BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNcLineCount()), TEXT_10));
        projStats.addCell(PdfText.of("Classes:", BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNumClasses()), TEXT_10));
        projStats.addCell();
        projStats.addCell(PdfText.of("Files:", BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNumFiles()), TEXT_10));
        if (metrics instanceof ProjectMetrics) {
            projStats.addCell(PdfText.of("Pkgs:", BOLD_10));
            projStats.addCell(PdfText.of(
                    Formatting.formatInt(((ProjectMetrics) metrics).getNumPackages()), TEXT_10));
        } else {
            projStats.addCell();
            projStats.addCell();
        }
        return projStats;
    }

    public static PdfTable createReportHeader(HasMetrics hasMetrics, long timestamp, String title,
                                              String titleAnchor, PDFColours colours) {
        final PdfTable titleBar = new PdfTable(2);
        final BlockMetrics metrics = hasMetrics.getMetrics();

        titleBar.setWidths(new int[]{50, 50});
        titleBar.setWidthPercentage(100.0);
        titleBar.getDefaultStyle()
                .setBorderColour(colours.COL_TABLE_BORDER)
                .setBackgroundColour(colours.COL_HEADER_BG)
                .setPaddingLeft(2.0)
                .setLeading(2.0, 0.9);

        final PdfTextBuilder titleText = PdfText.builder().add("OpenClover Coverage Report", BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);

        titleText.add("\nCoverage timestamp: ", BOLD_10);
        titleText.add(Formatting.formatDate(new Date(timestamp)), TEXT_10);

        titleBar.addCell(titleText.build());
        titleBar.addCell(createHeaderStats((PackageMetrics) metrics, colours));
        return titleBar;
    }

    /**
     * Appends the user-supplied report title, as a link when a title anchor was configured.
     */
    private static void appendReportTitle(PdfTextBuilder target, String title, String titleAnchor,
                                          PDFColours colours) {
        if (title != null && !title.trim().isEmpty()) {
            if (titleAnchor != null && !titleAnchor.trim().isEmpty()) {
                target.addLink("\n" + title,
                        PdfFontSpec.sans(12, PdfFontStyle.BOLD, colours.COL_LINK_TEXT), titleAnchor);
            } else {
                target.add("\n" + title, BOLD_12);
            }
        }
    }

    public static PdfTable createHistoricalPageHeader(String title, String titleAnchor, PDFColours colours) {
        final PdfTable titleBar = new PdfTable(1);

        titleBar.setWidths(new int[]{100});
        titleBar.setWidthPercentage(100.0);
        titleBar.getDefaultStyle()
                .setBorderColour(colours.COL_TABLE_BORDER)
                .setBackgroundColour(colours.COL_HEADER_BG);

        final PdfTable leftTab = new PdfTable(1);
        leftTab.setWidths(new int[]{100});
        leftTab.getDefaultStyle()
                .setBorders(PdfBorder.NONE)
                .setBackgroundColour(colours.COL_HEADER_BG)
                .setPaddingLeft(2.0)
                .setLeading(2.0, 0.9);

        final PdfTextBuilder titleText = PdfText.builder().add("Historical Coverage Report", BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);

        leftTab.addCell(titleText.build());
        titleBar.addCell(leftTab);
        return titleBar;
    }

    public static PdfTable createHistoricalReportHeader(HasMetrics hasMetrics, long ts1, long ts2,
                                                        String title, String titleAnchor,
                                                        PDFColours colours) {
        final PdfTable titleBar = new PdfTable(2);
        final BlockMetrics metrics = hasMetrics.getMetrics();

        titleBar.setWidths(new int[]{50, 50});
        titleBar.setWidthPercentage(100.0);
        titleBar.getDefaultStyle()
                .setBorderColour(colours.COL_TABLE_BORDER)
                .setBackgroundColour(colours.COL_HEADER_BG);

        final PdfTable leftTab = new PdfTable(2);
        leftTab.setWidths(new int[]{15, 85});
        leftTab.getDefaultStyle()
                .setBorders(PdfBorder.NONE)
                .setBackgroundColour(colours.COL_HEADER_BG)
                .setPaddingLeft(2.0)
                .setLeading(2.0, 0.9);

        final PdfTextBuilder titleText = PdfText.builder().add("Historical Coverage Report", BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);
        // the title spans both columns; the timestamps below it are label/value pairs
        leftTab.addCell(titleText.build(), FULL_WIDTH);

        leftTab.addCell(PdfText.of("From: ", BOLD_10), RIGHT_ALIGNED);
        leftTab.addCell(PdfText.of(Formatting.formatDate(new Date(ts1)), TEXT_10));
        leftTab.addCell(PdfText.of("To: ", BOLD_10), RIGHT_ALIGNED);
        leftTab.addCell(PdfText.of(Formatting.formatDate(new Date(ts2)), TEXT_10));

        titleBar.addCell(leftTab);
        titleBar.addCell(createHeaderStats((PackageMetrics) metrics, colours));
        return titleBar;
    }

    public static PdfTable createMoversTable(HistoricalReportDescriptor.MoversDescriptor moversDesc,
                                             PDFColours colours) {
        final List<MetricsDiffSummary> gainers = moversDesc.getGainers();
        final List<MetricsDiffSummary> losers = moversDesc.getLosers();
        final String requestedPeriod = moversDesc.getRequestedInterval().toSensibleString();
        final String period = moversDesc.getActualInterval().toSensibleString();
        final Percentage threshold = moversDesc.getThreshold();
        final int range = moversDesc.getRange();

        final PdfTable movers = newMoversTable(colours);

        final PdfText title = PdfText.builder()
                .add("Top movers over the last " + requestedPeriod, BOLD_12)
                .add(" (Actual Interval: " + period + ", Range: " + range
                        + ", Threshold: +/-" + threshold + ")", ITALIC_8)
                .build();

        addMoversTitle(movers, title, colours);

        if (gainers.isEmpty() && losers.isEmpty()) {
            movers.addCell(PdfText.of("No changes in coverage are outside the specified threshold (+/-"
                    + threshold + ")", ITALIC_10), FULL_WIDTH);
            return movers;
        }

        if (gainers.isEmpty()) {
            // there are no gainers.
            movers.addCell(PdfText.of("No classes have gained coverage over threshold (+"
                    + threshold + ")", ITALIC_10), FULL_WIDTH);
        } else {
            for (MetricsDiffSummary diff : gainers) {
                movers.addCell(PdfText.of(diff.getName(), TEXT_8));
                movers.addCell(createPCDiffBar(diff, colours));
            }
        }
        if (losers.isEmpty()) {
            // there are no losers.
            movers.addCell(PdfText.of("No classes have lost coverage over threshold (-"
                    + threshold + ")", ITALIC_10), FULL_WIDTH);
        } else {
            // losers are laid out mirrored: the bar first, then the class name
            for (MetricsDiffSummary diff : losers) {
                movers.addCell(createPCDiffBar(diff, colours));
                movers.addCell(PdfText.of(diff.getName(), TEXT_8));
            }
        }
        return movers;
    }

    public static PdfTable createAddedTable(HistoricalReportDescriptor.AddedDescriptor addedDesc,
                                            PDFColours colours) {
        final List<MetricsDiffSummary> gainers = addedDesc.getGainers();
        final String requestedPeriod = addedDesc.getRequestedInterval().toSensibleString();
        final String period = addedDesc.getActualInterval().toSensibleString();
        final int range = addedDesc.getRange();

        final PdfTable added = newMoversTable(colours);

        final PdfText title = PdfText.builder()
                .add("Classes added over the last " + requestedPeriod, BOLD_12)
                .add(" (Actual Interval: " + period + ", Range: " + range + ")", ITALIC_8)
                .build();

        addMoversTitle(added, title, colours);

        if (gainers.isEmpty()) {
            added.addCell(PdfText.of("No new classes", ITALIC_10), FULL_WIDTH);
            return added;
        }

        for (MetricsDiffSummary diff : gainers) {
            added.addCell(PdfText.of(diff.getName(), TEXT_8));
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
        return new CoverageBarWidget(pc, height, BAR_PADDING_RATIO, colours);
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
            header.addCell(PdfText.of(col0Title, BOLD_10));
            header.addCell(PdfText.of("Branch", BOLD_10), CENTERED);
            header.addCell(PdfText.of("Stmt", BOLD_10), CENTERED);
            header.addCell(PdfText.of("Method", BOLD_10), CENTERED);
            // "Total" heads both the percentage and the bar beside it
            header.addCell(PdfText.of("Total", BOLD_10), FULL_WIDTH);
        } else {
            header.setWidths(calculateEqualColumnWidths(numColumns));

            // use user-defined set of columns + one for a package name
            header.addCell(PdfText.of(col0Title, BOLD_10));
            cfg.getColumns().getPkgColumns()
                    .forEach(column -> header.addCell(PdfText.of(column.getName(), BOLD_10)));
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
}
