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
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfFontStyle;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.util.CloverChartFactory;
import org.openclover.core.reporters.util.HistoricalReportDescriptor;
import org.openclover.core.reporters.util.MetricsDiffSummary;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.util.Formatting;

import java.awt.Color;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    private static final float BAR_PADDING_RATIO = 0.01f;

    public static PdfTable getSpacerRow() {
        final PdfTable spacer = new PdfTable(1);
        spacer.getDefaultCell().setBorders(PdfBorder.NONE);
        spacer.setWidthPercentage(100f);
        spacer.addCell(PdfText.of(" ", TEXT_10));
        return spacer;
    }

    public static PdfTable createReportHeader(ProjectInfo hasmetrics, long ts, String title,
                                              String titleAnchor, PDFColours colours) {
        return createReportHeader(hasmetrics, ts, title, titleAnchor, true, colours);
    }

    public static PdfTable createReportHeader(PackageInfo hasmetrics, long ts, String title,
                                              String titleAnchor, PDFColours colours) {
        return createReportHeader(hasmetrics, ts, title, titleAnchor, false, colours);
    }

    public static PdfTable createCoverageDataTable(final CloverReportConfig cfg, final String col0Title,
                                                   final List<? extends HasMetrics> items,
                                                   final PDFColours colours) {
        final PdfTable stats = createCoverageDataHeader(cfg, col0Title, colours);

        for (HasMetrics item : items) {
            final BlockMetrics met = item.getMetrics();

            if (!cfg.getFormat().getShowEmpty() && met.getNumElements() == 0) {
                continue;
            }

            stats.getDefaultCell().setBorderColour(colours.COL_TABLE_BORDER);
            stats.getDefaultCell().setBorders(PdfBorder.BOX);
            stats.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.LEFT);
            stats.getDefaultCell().setVerticalAlignment(PdfAlign.Vertical.MIDDLE);

            // project or package name - always add
            String name = item.getName();
            if (name == null) {
                name = "Project"; //##HACK !!!!!
            }
            stats.addCell(PdfText.of(name, TEXT_10));

            if (!cfg.isColumnsSet()) {
                // use standard set of columns
                stats.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.CENTER);
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredBranches()), TEXT_10));
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredStatements()), TEXT_10));
                stats.addCell(PdfText.of(
                        Formatting.getPercentStr(((ClassMetrics) met).getPcCoveredMethods()), TEXT_10));
                stats.getDefaultCell().setBorders(PdfBorder.TOP | PdfBorder.BOTTOM);
                stats.addCell(PdfText.of(Formatting.getPercentStr(met.getPcCoveredElements()), BOLD_10));
                stats.getDefaultCell().setBorders(PdfBorder.TOP | PdfBorder.BOTTOM | PdfBorder.RIGHT);
                stats.addCell(createPCBar(met.getPcCoveredElements(), 10, colours));
            } else {
                // use user-defined set of columns
                for (Column column : cfg.getColumns().getPkgColumns()) {
                    // render as bar, percentage or a raw value
                    if (column.getFormat() instanceof ColumnFormat.BarGraphColumnFormat) {
                        stats.addCell(createPCBar(fetchPercentageValue(column, met), 10, colours));
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
     * @return float [0.0 ... 1.0] or -1.0 in case of error
     */
    private static float fetchPercentageValue(Column column, BlockMetrics metrics) {
        float value;
        try {
            column.init(metrics);
            value = column.getNumber().floatValue() / 100.0f;
            column.reset();
        } catch (CloverException ex) {
            value = -1.0f;
        }
        return value;
    }

    public static PdfTable createChart(Historical.Chart chartCfg, Map<Long, ? extends HasMetrics> data,
                                       PDFColours colours) {
        final PdfTable coverage = new PdfTable(1);
        coverage.setWidthPercentage(100);
        coverage.getDefaultCell().setBorderColour(colours.COL_TABLE_BORDER);
        coverage.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);
        coverage.getDefaultCell().setPaddingLeft(2);
        coverage.addCell(PdfText.of(" ", BOLD_12));

        coverage.getDefaultCell().setBackgroundColour(Color.white);

        final JFreeChart graph = CloverChartFactory.createJFreeChart(chartCfg, data);
        coverage.addCell(new ChartWidget(graph, chartCfg.getHeight()));
        return coverage;
    }

    private static PdfTable createHeaderStats(PackageMetrics metrics, PDFColours colours) {
        final String level = metrics.getType();

        final PdfTable projStats = new PdfTable(5);
        projStats.setWidths(new int[]{30, 17, 18, 20, 15});
        projStats.getDefaultCell().setBorders(PdfBorder.NONE);
        projStats.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.RIGHT);
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

    public static PdfTable createReportHeader(HasMetrics hasmetrics, long timestamp, String title,
                                              String titleAnchor, boolean isProject, PDFColours colours) {
        final PdfTable titlebar = new PdfTable(2);
        final BlockMetrics metrics = hasmetrics.getMetrics();

        titlebar.setWidths(new int[]{50, 50});
        titlebar.setWidthPercentage(100);
        titlebar.getDefaultCell().setBorderColour(colours.COL_TABLE_BORDER);
        titlebar.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);
        titlebar.getDefaultCell().setPaddingLeft(2);
        titlebar.getDefaultCell().setLeading(2, 0.9f);

        final PdfText titleText = PdfText.of("OpenClover Coverage Report", BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);

        titleText.add("\nCoverage timestamp: ", BOLD_10);
        titleText.add(Formatting.formatDate(new Date(timestamp)), TEXT_10);

        titlebar.addCell(titleText);
        titlebar.addCell(createHeaderStats((PackageMetrics) metrics, colours));
        return titlebar;
    }

    /**
     * Appends the user-supplied report title, as a link when a title anchor was configured.
     */
    private static void appendReportTitle(PdfText target, String title, String titleAnchor,
                                          PDFColours colours) {
        if (title != null && title.trim().length() > 0) {
            if (titleAnchor != null && titleAnchor.trim().length() > 0) {
                target.addLink("\n" + title,
                        PdfFontSpec.sans(12, PdfFontStyle.BOLD, colours.COL_LINK_TEXT), titleAnchor);
            } else {
                target.add("\n" + title, BOLD_12);
            }
        }
    }

    public static PdfTable createHistoricalPageHeader(String title, String titleAnchor, PDFColours colours) {
        final PdfTable titlebar = new PdfTable(1);

        titlebar.setWidths(new int[]{100});
        titlebar.setWidthPercentage(100);
        titlebar.getDefaultCell().setBorderColour(colours.COL_TABLE_BORDER);
        titlebar.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);

        final PdfTable leftTab = new PdfTable(1);
        leftTab.setWidths(new int[]{100});
        leftTab.getDefaultCell().setBorders(PdfBorder.NONE);
        leftTab.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);
        leftTab.getDefaultCell().setPaddingLeft(2);
        leftTab.getDefaultCell().setLeading(2, 0.9f);

        final PdfText titleText = PdfText.of("Historical Coverage Report", BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);

        leftTab.addCell(titleText);
        titlebar.addCell(leftTab);
        return titlebar;
    }

    public static PdfTable createHistoricalReportHeader(HasMetrics hasmetrics, long ts1, long ts2,
                                                        String title, String titleAnchor,
                                                        boolean isProject, PDFColours colours) {
        final PdfTable titlebar = new PdfTable(2);
        final BlockMetrics metrics = hasmetrics.getMetrics();

        titlebar.setWidths(new int[]{50, 50});
        titlebar.setWidthPercentage(100);
        titlebar.getDefaultCell().setBorderColour(colours.COL_TABLE_BORDER);
        titlebar.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);

        final PdfTable leftTab = new PdfTable(2);
        leftTab.setWidths(new int[]{15, 85});
        leftTab.getDefaultCell().setBorders(PdfBorder.NONE);
        leftTab.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);
        leftTab.getDefaultCell().setPaddingLeft(2);
        leftTab.getDefaultCell().setLeading(2, 0.9f);
        leftTab.getDefaultCell().setColspan(2);

        final PdfText titleText = PdfText.of("Historical Coverage Report", BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);
        leftTab.addCell(titleText);

        leftTab.getDefaultCell().setColspan(1);
        leftTab.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.RIGHT);
        leftTab.addCell(PdfText.of("From: ", BOLD_10));
        leftTab.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.LEFT);
        leftTab.addCell(PdfText.of(Formatting.formatDate(new Date(ts1)), TEXT_10));
        leftTab.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.RIGHT);
        leftTab.addCell(PdfText.of("To: ", BOLD_10));
        leftTab.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.LEFT);
        leftTab.addCell(PdfText.of(Formatting.formatDate(new Date(ts2)), TEXT_10));

        titlebar.addCell(leftTab);
        titlebar.addCell(createHeaderStats((PackageMetrics) metrics, colours));
        return titlebar;
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

        final PdfText title = PdfText.of("Top movers over the last " + requestedPeriod, BOLD_12);
        title.add(" (Actual Interval: " + period + ", Range: " + range
                + ", Threshold: +/-" + threshold + ")", ITALIC_8);

        movers.addCell(title);
        movers.getDefaultCell().setBackgroundColour(Color.white);

        if (gainers.size() == 0 && losers.size() == 0) {
            movers.getDefaultCell().setColspan(2);
            movers.addCell(PdfText.of("No changes in coverage are outside the specified threshold (+/-"
                    + threshold + ")", ITALIC_10));
            return movers;
        }

        movers.getDefaultCell().setColspan(1);
        if (gainers.size() == 0) {
            // there are no gainers.
            movers.getDefaultCell().setColspan(2);
            movers.addCell(PdfText.of("No classes have gained coverage over threshold (+"
                    + threshold + ")", ITALIC_10));
        } else {
            for (MetricsDiffSummary diff : gainers) {
                movers.addCell(PdfText.of(diff.getName(), TEXT_8));
                movers.addCell(createPCDiffBar(diff, colours));
            }
        }
        movers.getDefaultCell().setColspan(1);
        if (losers.size() == 0) {
            // there are no losers.
            movers.getDefaultCell().setColspan(2);
            movers.addCell(PdfText.of("No classes have lost coverage over threshold (-"
                    + threshold + ")", ITALIC_10));
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

        final PdfText title = PdfText.of("Classes added over the last " + requestedPeriod, BOLD_12);
        title.add(" (Actual Interval: " + period + ", Range: " + range + ")", ITALIC_8);

        added.addCell(title);
        added.getDefaultCell().setBackgroundColour(Color.white);

        if (gainers.size() == 0) {
            added.getDefaultCell().setColspan(2);
            added.addCell(PdfText.of("No new classes", ITALIC_10));
            return added;
        }

        added.getDefaultCell().setColspan(1);
        for (MetricsDiffSummary diff : gainers) {
            added.addCell(PdfText.of(diff.getName(), TEXT_8));
            added.addCell(createPCDiffBar(diff, colours));
        }
        return added;
    }

    private static PdfTable newMoversTable(PDFColours colours) {
        final PdfTable movers = new PdfTable(2);
        movers.setWidthPercentage(100f);
        movers.getDefaultCell().setBorderColour(colours.COL_TABLE_BORDER);
        movers.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);
        movers.setWidths(new int[]{50, 50});
        movers.getDefaultCell().setColspan(2);
        return movers;
    }

    private static CoverageDiffBarWidget createPCDiffBar(MetricsDiffSummary diff, PDFColours colours) {
        return new CoverageDiffBarWidget(diff.getPcDiff(), diff.getPc2float(), 8, colours);
    }

    private static CoverageBarWidget createPCBar(float pc, float height, PDFColours colours) {
        return new CoverageBarWidget(pc, height, BAR_PADDING_RATIO, colours);
    }

    private static PdfTable createCoverageDataHeader(final CloverReportConfig cfg, final String col0Title,
                                                     final PDFColours colours) {
        final int numColumns = !cfg.isColumnsSet() ? 6 : 1 + cfg.getColumns().getPkgColumns().size();
        final PdfTable header = new PdfTable(numColumns);
        header.setWidthPercentage(100f);
        header.getDefaultCell().setBorderColour(colours.COL_TABLE_BORDER);
        header.getDefaultCell().setBackgroundColour(colours.COL_HEADER_BG);

        if (!cfg.isColumnsSet()) {
            // use standard set of columns: package name, Branch, Stmt, Method, Total
            header.setWidths(new int[]{50, 10, 10, 10, 7, 13});
            header.addCell(PdfText.of(col0Title, BOLD_10));
            header.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.CENTER);
            header.addCell(PdfText.of("Branch", BOLD_10));
            header.addCell(PdfText.of("Stmt", BOLD_10));
            header.addCell(PdfText.of("Method", BOLD_10));
            header.getDefaultCell().setColspan(2);
            header.getDefaultCell().setHorizontalAlignment(PdfAlign.Horizontal.LEFT);
            header.addCell(PdfText.of("Total", BOLD_10));
        } else {
            header.setWidths(calculateEqualColumnWidths(numColumns));

            // use user-defined set of columns + one for a package name
            header.addCell(PdfText.of(col0Title, BOLD_10));
            for (Column column : cfg.getColumns().getPkgColumns()) {
                header.addCell(PdfText.of(column.getName(), BOLD_10));
            }
        }

        // leave the table in a friendly state for additions
        header.getDefaultCell().setColspan(1);
        header.getDefaultCell().setBackgroundColour(Color.white);

        return header;
    }

    private static int[] calculateEqualColumnWidths(int numColumns) {
        // first column =  50%, others = same width
        final int[] columnWidths = new int[numColumns];
        columnWidths[0] = 50;
        for (int i = 1; i < columnWidths.length; i++) {
            columnWidths[i] = 50 / (columnWidths.length - 1);
        }
        return columnWidths;
    }
}
