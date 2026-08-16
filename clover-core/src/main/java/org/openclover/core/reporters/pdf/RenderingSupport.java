package org.openclover.core.reporters.pdf;

import org.jfree.chart.JFreeChart;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.Historical;
import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfCellStyleBuilder;
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfFontStyle;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.util.CloverChartFactory;
import org.openclover.core.reporters.util.HistoricalReportDescriptor;

import java.awt.Color;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the tables the PDF reports are made of — one entry point per report element, which is all
 * the reporters need to know about.
 *
 * <p>The tables themselves are built by {@link ReportHeaderTables} and {@link CoverageTables}. The
 * fonts and style deviations below live here rather than in either of them because both use them.
 */
public class RenderingSupport {

    static final PdfFontSpec TEXT_8 = PdfFontSpec.sans(8);
    static final PdfFontSpec ITALIC_8 = PdfFontSpec.sans(8, PdfFontStyle.ITALIC);
    static final PdfFontSpec TEXT_10 = PdfFontSpec.sans(10);
    static final PdfFontSpec BOLD_10 = PdfFontSpec.sans(10, PdfFontStyle.BOLD);
    static final PdfFontSpec ITALIC_10 = PdfFontSpec.sans(10, PdfFontStyle.ITALIC);
    static final PdfFontSpec BOLD_12 = PdfFontSpec.sans(12, PdfFontStyle.BOLD);
    static final PdfFontSpec BOLD_14 = PdfFontSpec.sans(14, PdfFontStyle.BOLD);

    /** Fraction of a cell's width left blank on either side of a coverage bar. */
    static final double BAR_PADDING_RATIO = 0.01;

    /** Height of the coverage bars, in points. */
    static final double BAR_HEIGHT = 10.0;

    /** Style deviations used often enough to be worth naming. */
    static final Consumer<PdfCellStyleBuilder> CENTERED =
            style -> style.setHorizontalAlignment(PdfAlign.Horizontal.CENTER);

    static final Consumer<PdfCellStyleBuilder> RIGHT_ALIGNED =
            style -> style.setHorizontalAlignment(PdfAlign.Horizontal.RIGHT);

    /** A cell spanning both columns of a two column table. */
    static final Consumer<PdfCellStyleBuilder> FULL_WIDTH = style -> style.setColspan(2);

    public static PdfTable getSpacerRow() {
        final PdfTable spacer = new PdfTable(1);
        spacer.getDefaultStyle().setBorders(PdfBorder.NONE);
        spacer.setWidthPercentage(100.0);
        spacer.addCell(PdfText.of(" ", TEXT_10));
        return spacer;
    }

    public static PdfTable createReportHeader(ProjectInfo hasMetrics, long ts, String title,
                                              String titleAnchor, PDFColours colours) {
        return ReportHeaderTables.createReportHeader(hasMetrics, ts, title, titleAnchor, colours);
    }

    public static PdfTable createReportHeader(PackageInfo hasMetrics, long ts, String title,
                                              String titleAnchor, PDFColours colours) {
        return ReportHeaderTables.createReportHeader(hasMetrics, ts, title, titleAnchor, colours);
    }

    public static PdfTable createReportHeader(HasMetrics hasMetrics, long timestamp, String title,
                                              String titleAnchor, PDFColours colours) {
        return ReportHeaderTables.createReportHeader(hasMetrics, timestamp, title, titleAnchor, colours);
    }

    public static PdfTable createHistoricalPageHeader(String title, String titleAnchor,
                                                      PDFColours colours) {
        return ReportHeaderTables.createHistoricalPageHeader(title, titleAnchor, colours);
    }

    public static PdfTable createHistoricalReportHeader(HasMetrics hasMetrics, long ts1, long ts2,
                                                        String title, String titleAnchor,
                                                        PDFColours colours) {
        return ReportHeaderTables.createHistoricalReportHeader(
                hasMetrics, ts1, ts2, title, titleAnchor, colours);
    }

    public static PdfTable createCoverageDataTable(CloverReportConfig cfg, String col0Title,
                                                   java.util.List<? extends HasMetrics> items,
                                                   PDFColours colours) {
        return CoverageTables.createCoverageDataTable(cfg, col0Title, items, colours);
    }

    public static PdfTable createMoversTable(HistoricalReportDescriptor.MoversDescriptor moversDesc,
                                             PDFColours colours) {
        return CoverageTables.createMoversTable(moversDesc, colours);
    }

    public static PdfTable createAddedTable(HistoricalReportDescriptor.AddedDescriptor addedDesc,
                                            PDFColours colours) {
        return CoverageTables.createAddedTable(addedDesc, colours);
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
}
