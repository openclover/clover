package com.atlassian.clover.reporters.pdf;

import com.atlassian.clover.CloverLicenseInfo;
import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.cfg.Percentage;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.metrics.ClassMetrics;
import com.atlassian.clover.registry.metrics.PackageMetrics;
import com.atlassian.clover.registry.metrics.ProjectMetrics;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.reporters.ColumnFormat;
import com.atlassian.clover.reporters.Historical;
import com.atlassian.clover.reporters.util.CloverChartFactory;
import com.atlassian.clover.reporters.util.HistoricalReportDescriptor;
import com.atlassian.clover.reporters.util.MetricsDiffSummary;
import org.openclover.runtime.util.Formatting;
import com.atlassian.clover.util.format.PDFFormatter;
import clover.com.lowagie.text.Chunk;
import clover.com.lowagie.text.DocumentException;
import clover.com.lowagie.text.Element;
import clover.com.lowagie.text.Font;
import clover.com.lowagie.text.FontFactory;
import clover.com.lowagie.text.Phrase;
import clover.com.lowagie.text.Rectangle;
import clover.com.lowagie.text.pdf.DefaultFontMapper;
import clover.com.lowagie.text.pdf.PdfContentByte;
import clover.com.lowagie.text.pdf.PdfPTable;
import clover.com.lowagie.text.pdf.PdfPTableEvent;
import clover.com.lowagie.text.pdf.PdfTemplate;
import clover.org.jfree.chart.JFreeChart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RenderingSupport {

    private static final PdfPTable SPACER = new PdfPTable(1);

    static {
        SPACER.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        SPACER.setWidthPercentage(100f);
        SPACER.addCell(" ");
    }


    public static PdfPTable createLicenseWarningBar(String font, int points, PDFColours colours) {
        PdfPTable warnTab = new PdfPTable(1);
        warnTab.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        warnTab.getDefaultCell().setPadding(5);
        warnTab.setWidthPercentage(100f);

        // The warning bar only contains details when the license is verbose.
        
        String stmt = CloverLicenseInfo.OWNER_STMT + " ";
        if (!CloverLicenseInfo.EXPIRED) {
            stmt = stmt + CloverLicenseInfo.PRE_EXPIRY_STMT;
        } else {
            stmt = stmt + CloverLicenseInfo.POST_EXPIRY_STMT +
                    " " + CloverLicenseInfo.CONTACT_INFO_STMT;
        }


        Phrase warning = PDFFormatter.format(stmt, font, points, colours.COL_LINK_TEXT);

        warnTab.addCell(warning);
        return warnTab;
    }

    public static PdfPTable getSpacerRow() {
        return SPACER;
    }

    public static PdfPTable createReportHeader(FullProjectInfo hasmetrics, long ts, String title, String titleAnchor, PDFColours colours)
            throws DocumentException {
        return createReportHeader(hasmetrics, ts, title, titleAnchor, true, colours);
    }

    public static PdfPTable createReportHeader(FullPackageInfo hasmetrics, long ts, String title, String titleAnchor, PDFColours colours)
            throws DocumentException {
        return createReportHeader(hasmetrics, ts, title, titleAnchor, false, colours);
    }

    public static PdfPTable createCoverageDataTable(final CloverReportConfig cfg, final String col0Title,
                                                    final List<HasMetrics> items, final PDFColours colours)
            throws DocumentException {

        final PdfPTable stats = createCoverageDataHeader(cfg, col0Title, colours);

        for (HasMetrics item : items) {
            final com.atlassian.clover.api.registry.BlockMetrics met = item.getMetrics();

            if (!cfg.getFormat().getShowEmpty() && met.getNumElements() == 0) {
                continue;
            }

            stats.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
            stats.getDefaultCell().setBorder(Rectangle.BOX);
            stats.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            stats.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);

            // project or package name - always add
            String name = item.getName();
            if (name == null) {
                name = "Project"; //##HACK !!!!!
            }
            stats.addCell(new Phrase(name, FontFactory.getFont(FontFactory.HELVETICA, 10)));

            if (!cfg.isColumnsSet()) {
                // use standard set of columns
                stats.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
                stats.addCell(new Phrase(Formatting.getPercentStr(met.getPcCoveredBranches()),
                        FontFactory.getFont(FontFactory.HELVETICA, 10)));
                stats.addCell(new Phrase(Formatting.getPercentStr(met.getPcCoveredStatements()),
                        FontFactory.getFont(FontFactory.HELVETICA, 10)));
                stats.addCell(new Phrase(Formatting.getPercentStr(((ClassMetrics) met).getPcCoveredMethods()),
                        FontFactory.getFont(FontFactory.HELVETICA, 10)));
                stats.getDefaultCell().setBorder(Rectangle.TOP | Rectangle.BOTTOM);
                stats.addCell(new Phrase(Formatting.getPercentStr(met.getPcCoveredElements()),
                        FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
                stats.getDefaultCell().setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.RIGHT);
                stats.addCell(createPCBar(met.getPcCoveredElements(), 10, colours));
            } else {
                // use user-defined set of columns
                for (Column column : cfg.getColumns().getPkgColumns()) {
                    // render as bar, percentage or a raw value
                    if (column.getFormat() instanceof ColumnFormat.BarGraphColumnFormat) {
                        stats.addCell(createPCBar(fetchPercentageValue(column, met), 10, colours));
                    } else {
                        stats.addCell(new Phrase(renderValue(column, met), FontFactory.getFont(FontFactory.HELVETICA, 10)));
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

    public static PdfPTable createChart(Historical.Chart chartCfg, Map data, PDFColours colours) {
        final PdfPTable coverage = new PdfPTable(1);
        coverage.setWidthPercentage(100);
        coverage.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
        coverage.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);
        coverage.getDefaultCell().setPaddingLeft(2);
        Phrase headerPhrase = new Phrase(" ",
                FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD));
        coverage.addCell(headerPhrase);
        coverage.getDefaultCell().setBackgroundColor(Color.white);

        final PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);

        final JFreeChart graph = CloverChartFactory.createJFreeChart(chartCfg, data);

        final GraphRenderer renderer = new GraphRenderer(graph);
        chartTable.setTableEvent(renderer);
        chartTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        chartTable.getDefaultCell().setMinimumHeight(chartCfg.getHeight());
        chartTable.addCell("");
        coverage.addCell(chartTable);
        return coverage;
    }

    private static PdfPTable createHeaderStats(PackageMetrics metrics, PDFColours colours)
            throws DocumentException {
        String level = metrics.getType();

        PdfPTable projStats = new PdfPTable(5);
        projStats.setWidths(new int[]{30, 17, 18, 20, 15});
        projStats.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        projStats.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
        projStats.addCell(new Phrase(level + " stats:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        projStats.addCell(new Phrase("LOC:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        projStats.addCell(new Phrase(Formatting.formatInt(metrics.getLineCount()),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        projStats.addCell(new Phrase("Methods:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        projStats.addCell(new Phrase(Formatting.formatInt(metrics.getNumMethods()),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        projStats.addCell("");
        projStats.addCell(new Phrase("NCLOC:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        projStats.addCell(new Phrase(Formatting.formatInt(metrics.getNcLineCount()),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        projStats.addCell(new Phrase("Classes:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        projStats.addCell(new Phrase(Formatting.formatInt(metrics.getNumClasses()),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        projStats.addCell("");
        projStats.addCell(new Phrase("Files:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        projStats.addCell(new Phrase(Formatting.formatInt(metrics.getNumFiles()),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        if (metrics instanceof ProjectMetrics) {
            projStats.addCell(new Phrase("Pkgs:", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
            projStats.addCell(new Phrase(Formatting.formatInt(((ProjectMetrics)metrics).getNumPackages()),
                    FontFactory.getFont(FontFactory.HELVETICA, 10)));
        } else {
            projStats.addCell("");
            projStats.addCell("");
        }
        return projStats;
    }

    public static PdfPTable createReportHeader(HasMetrics hasmetrics, long timestamp, String title,
                                               boolean isProject, PDFColours colours) throws DocumentException {
        return createReportHeader(hasmetrics, timestamp, title, null, isProject, colours);
    }


    public static PdfPTable createReportHeader(HasMetrics hasmetrics, long timestamp, String title, String titleAnchor,
                                               boolean isProject, PDFColours colours)
            throws DocumentException {
        PdfPTable titlebar = new PdfPTable(2);
        com.atlassian.clover.api.registry.BlockMetrics metrics = hasmetrics.getMetrics();

        titlebar.setWidths(new int[]{50, 50});
        titlebar.setWidthPercentage(100);
        titlebar.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
        titlebar.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);
        titlebar.getDefaultCell().setPaddingLeft(2);
        titlebar.getDefaultCell().setLeading(2, 0.9f);

        Phrase titlePhrase = new Phrase(14, "Clover Coverage Report",
                FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD));

        //checks title present and valid
        if (title != null && title.trim().length() > 0) {
            //checks titleAnchor present and valid
            if (titleAnchor != null && titleAnchor.trim().length() > 0){
                //hyperlink to project page
                titlePhrase.add(new Chunk("\n" + title, FontFactory.getFont(FontFactory.HELVETICA, 12,
                             Font.BOLD, colours.COL_LINK_TEXT)).setAnchor(titleAnchor));
            }
            else {
                //normal title
                titlePhrase.add(new Phrase("\n" + title, FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
            }
        }

        titlePhrase.add(new Phrase("\nCoverage timestamp: ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        titlePhrase.add(new Phrase(Formatting.formatDate(new Date(timestamp)),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));

        titlebar.addCell(titlePhrase);
        titlebar.addCell(createHeaderStats((PackageMetrics)metrics, colours));
        return titlebar;
    }

    public static PdfPTable createHistoricalPageHeader(String title, PDFColours colours) throws DocumentException{
        return createHistoricalPageHeader(title, null, colours);
    }

    public static PdfPTable createHistoricalPageHeader(String title, String titleAnchor, PDFColours colours)
            throws DocumentException {
        PdfPTable titlebar = new PdfPTable(1);

        titlebar.setWidths(new int[]{100});
        titlebar.setWidthPercentage(100);
        titlebar.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
        titlebar.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);

        PdfPTable leftTab = new PdfPTable(1);
        leftTab.setWidths(new int[]{100});
        leftTab.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        leftTab.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);
        leftTab.getDefaultCell().setPaddingLeft(2);
        leftTab.getDefaultCell().setLeading(2, 0.9f);
        leftTab.getDefaultCell().setColspan(1);
        Phrase titlePhrase = new Phrase(14, "Historical Coverage Report",
                FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD));

        if (title != null && title.trim().length() > 0) {
            if (titleAnchor != null && titleAnchor.trim().length() >0){
                titlePhrase.add(new Chunk("\n" + title, FontFactory.getFont(FontFactory.HELVETICA, 12,
                     Font.BOLD, colours.COL_LINK_TEXT)).setAnchor(titleAnchor));
            }
            else {
                titlePhrase.add(new Phrase("\n" + title, FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
            }
        }
        leftTab.addCell(titlePhrase);
        titlebar.addCell(leftTab);
        return titlebar;
    }

    public static PdfPTable createHistoricalReportHeader(HasMetrics hasmetrics, long ts1, long ts2, String title, String titleAnchor,
                                                         boolean isProject, PDFColours colours) throws DocumentException {

        PdfPTable titlebar = new PdfPTable(2);
        com.atlassian.clover.api.registry.BlockMetrics metrics = hasmetrics.getMetrics();

        titlebar.setWidths(new int[]{50, 50});
        titlebar.setWidthPercentage(100);
        titlebar.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
        titlebar.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);

        PdfPTable leftTab = new PdfPTable(2);
        leftTab.setWidths(new int[]{15, 85});
        leftTab.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        leftTab.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);
        leftTab.getDefaultCell().setPaddingLeft(2);
        leftTab.getDefaultCell().setLeading(2, 0.9f);
        leftTab.getDefaultCell().setColspan(2);
        Phrase titlePhrase = new Phrase(14, "Historical Coverage Report",
                FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD));

        if (title != null && title.trim().length() > 0) {
            if (titleAnchor != null && titleAnchor.trim().length() >0){
                titlePhrase.add(new Chunk("\n" + title, FontFactory.getFont(FontFactory.HELVETICA, 12,
                     Font.BOLD, colours.COL_LINK_TEXT)).setAnchor(titleAnchor));
            }
            else {
                titlePhrase.add(new Phrase("\n" + title, FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
            }
        }
        leftTab.addCell(titlePhrase);
        leftTab.getDefaultCell().setColspan(1);
        leftTab.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);

        leftTab.addCell(new Phrase("From: ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        leftTab.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        leftTab.addCell(new Phrase(Formatting.formatDate(new Date(ts1)),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        leftTab.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
        leftTab.addCell(new Phrase("To: ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        leftTab.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        leftTab.addCell(new Phrase(Formatting.formatDate(new Date(ts2)),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        titlebar.addCell(leftTab);
        titlebar.addCell(createHeaderStats((PackageMetrics)metrics, colours));
        return titlebar;
    }


    public static PdfPTable createMoversTable(HistoricalReportDescriptor.MoversDescriptor moversDesc, PDFColours colours)
            throws DocumentException {

        List<MetricsDiffSummary> gainers = moversDesc.getGainers();
        List<MetricsDiffSummary> losers = moversDesc.getLosers();
        String requestedPeriod = moversDesc.getRequestedInterval().toSensibleString();
        String period = moversDesc.getActualInterval().toSensibleString();
        Percentage threshold = moversDesc.getThreshold();
        int range = moversDesc.getRange();

        PdfPTable movers = new PdfPTable(2);
        movers.setWidthPercentage(100f);
        movers.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
        movers.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);
        movers.setWidths(new int[]{50, 50});
        movers.getDefaultCell().setColspan(2);

        Phrase title = new Phrase("Top movers over the last " + requestedPeriod,
                FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD));
        title.add(new Phrase(" (Actual Interval: "+period+", Range: " + range + ", Threshold: +/-" + threshold + ")",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC)));

        movers.addCell(title);
        movers.getDefaultCell().setBackgroundColor(Color.white);


        if (gainers.size() == 0 && losers.size() == 0) {
            movers.getDefaultCell().setColspan(2);
            movers.addCell(new Phrase("No changes in coverage are outside the specified threshold (+/-"
                    + threshold + ")",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC)));

            return movers;
        }


        movers.getDefaultCell().setColspan(1);
        if (gainers.size() == 0) {
            // there are no gainers.
            movers.getDefaultCell().setColspan(2);
            movers.addCell(new Phrase("No classes have gained coverage over threshold (+"
                    + threshold + ")",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC)));
        } else {
            for (MetricsDiffSummary diff : gainers) {
                float pcdiff = diff.getPcDiff();
                float pcnow = diff.getPc2float();
                movers.addCell(new Phrase(diff.getName(),
                        FontFactory.getFont(FontFactory.HELVETICA, 8)));
                movers.addCell(createPCDiffBar(pcdiff, pcnow, 8, colours));
            }
        }
        movers.getDefaultCell().setColspan(1);
        if (losers.size() == 0) {
            // there are no losers.
            movers.getDefaultCell().setColspan(2);
            movers.addCell(new Phrase("No classes have lost coverage over threshold (-"
                    + threshold + ")",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC)));
        } else {
            for (MetricsDiffSummary diff : losers) {
                float pcdiff = diff.getPcDiff();
                float pcnow = diff.getPc2float();
                movers.addCell(createPCDiffBar(pcdiff, pcnow, 8, colours)); //##HACK
                movers.addCell(new Phrase(diff.getName(),
                        FontFactory.getFont(FontFactory.HELVETICA, 8)));
            }

        }
        return movers;
    }

    public static PdfPTable createAddedTable(HistoricalReportDescriptor.AddedDescriptor addedDesc, PDFColours colours)
            throws DocumentException {

        List<MetricsDiffSummary> gainers = addedDesc.getGainers();
        String requestedPeriod = addedDesc.getRequestedInterval().toSensibleString();
        String period = addedDesc.getActualInterval().toSensibleString();
        int range = addedDesc.getRange();

        PdfPTable movers = new PdfPTable(2);
        movers.setWidthPercentage(100f);
        movers.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
        movers.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);
        movers.setWidths(new int[]{50, 50});
        movers.getDefaultCell().setColspan(2);

        Phrase title = new Phrase("Classes added over the last " + requestedPeriod,
                FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD));
        title.add(new Phrase(" (Actual Interval: "+period+", Range: " + range + ")",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC)));

        movers.addCell(title);
        movers.getDefaultCell().setBackgroundColor(Color.white);


        if (gainers.size() == 0) {
            movers.getDefaultCell().setColspan(2);
            movers.addCell(new Phrase("No new classes", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC)));
            return movers;
        }

        movers.getDefaultCell().setColspan(1);
        for (MetricsDiffSummary diff : gainers) {
            float pcdiff = diff.getPcDiff();
            float pcnow = diff.getPc2float();
            movers.addCell(new Phrase(diff.getName(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8)));
            movers.addCell(createPCDiffBar(pcdiff, pcnow, 8, colours));
        }
        movers.getDefaultCell().setColspan(1);
        return movers;
    }

    private static PdfPTable createPCDiffBar(float pcdiff, float pcnow, float height, PDFColours colours)
            throws DocumentException {
        PdfPTable pcbar = new PdfPTable(2);
        pcbar.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        pcdiff = pcdiff/100f;

        if (pcdiff < 0) {
            PCBarRenderer renderer = new PCBarRenderer(1, 0, height, colours, 0);
            pcbar.setTableEvent(renderer);
            pcbar.setWidths(new float[]{20 + 80 * (1 - Math.abs(pcdiff)), 80 * Math.abs(pcdiff)});
            pcbar.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            pcbar.addCell(new Phrase("(" + Formatting.getPercentStr(pcnow) + ") " + Formatting.format1d(pcdiff * 100),
                    FontFactory.getFont(FontFactory.HELVETICA, height)));
            pcbar.addCell("");
        } else {
            PCBarRenderer renderer = new PCBarRenderer(0, 100, height, colours, 0);
            pcbar.setTableEvent(renderer);
            pcbar.setWidths(new float[]{80 * Math.abs(pcdiff), 20 + 80 * (1 - pcdiff)});
            pcbar.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            pcbar.addCell("");
            pcbar.addCell(new Phrase("+" + Formatting.format1d(pcdiff * 100) + " (" + Formatting.getPercentStr(pcnow) + ")",
                    FontFactory.getFont(FontFactory.HELVETICA, height)));
        }
        return pcbar;
    }


    private static PdfPTable createPCBar(float pc, float height, PDFColours colours) {
        return createPCBar(pc, height, colours, 0.01f);
    }

    private static PdfPTable createPCBar(float pc, float height, PDFColours colours, float padx) {
        PdfPTable pcbar = new PdfPTable(1);
        PCBarRenderer renderer = new PCBarRenderer(pc, height, colours, padx);
        //pcbar.setWidthPercentage(100); ##HACK - do we need this?
        pcbar.setTableEvent(renderer);
        pcbar.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        pcbar.addCell("");
        return pcbar;
    }

    private static PdfPTable createCoverageDataHeader(final CloverReportConfig cfg, final String col0Title, final PDFColours colours)
            throws DocumentException {
        final int numColumns = !cfg.isColumnsSet() ? 6 : 1 + cfg.getColumns().getPkgColumns().size();
        final PdfPTable header = new PdfPTable(numColumns);
        header.setWidthPercentage(100f);
        header.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
        header.getDefaultCell().setBackgroundColor(colours.COL_HEADER_BG);

        if (!cfg.isColumnsSet()) {
            // use standard set of columns: package name, Branch, Stmt, Method, Total
            header.setWidths(new int[]{50, 10, 10, 10, 7, 13});
            header.addCell(new Phrase(col0Title, FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
            header.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            header.addCell(new Phrase("Branch", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
            header.addCell(new Phrase("Stmt", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
            header.addCell(new Phrase("Method", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
            header.getDefaultCell().setColspan(2);
            header.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            header.addCell(new Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        } else {
            header.setWidths(calculateEqualColumnWidths(numColumns));

            // use user-defined set of columns + one for a package name
            header.addCell(new Phrase(col0Title, FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
            final List<Column> columns = cfg.getColumns().getPkgColumns();
            for (Column column : columns) {
                header.addCell(new Phrase(column.getName(), FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
            }
        }


        // leave the table in a friendly state for additions
        header.getDefaultCell().setColspan(1);
        header.getDefaultCell().setBackgroundColor(Color.white);

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

    static class GraphRenderer implements PdfPTableEvent {

        private JFreeChart graph;

        public GraphRenderer(JFreeChart graph) {
            this.graph = graph;
        }

        @Override
        public void tableLayout(PdfPTable table, float[][] width, float[] heights,
                                int headerRows, int rowStart, PdfContentByte[] canvases) {
            float[] widths = width[0];
            PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
            float w = widths[1] - widths[0];
            float h = heights[0] - heights[1];
            float x = widths[0];
            float y = heights[1];
            cb.saveState();

            PdfTemplate tp = cb.createTemplate(w, h);
            tp.setWidth(w);
            tp.setHeight(h);
            Graphics2D g2 = tp.createGraphics(w, h, new DefaultFontMapper());
            graph.draw(g2, new Rectangle2D.Float( 0, 0, (int) w, (int) h));
            g2.dispose();
            cb.addTemplate(tp, x, y);
            cb.restoreState();
        }

    }


    static class PCBarRenderer implements PdfPTableEvent {

        private float coveredpc;
        private float height;
        private float padx;
        private PDFColours colours;
        private int column = 0;

        public PCBarRenderer(float coveredpc, float height, PDFColours colours, float padx) {
            this(0, coveredpc, height, colours, padx);
        }


        public PCBarRenderer(int column, float coveredpc, float height, PDFColours colours, float padx) {
            this.column = column;
            this.coveredpc = (coveredpc > 1 ? 1 : coveredpc); // prevent rendering nasties
            this.height = height - 2; // ##HACK - we are passed the font height.
            this.colours = colours;
            this.padx = padx;
        }

        @Override
        public void tableLayout(PdfPTable table, float[][] width, float[] heights,
                                int headerRows, int rowStart, PdfContentByte[] canvases) {
            float[] widths = width[0];
            PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
            cb.saveState();

            cb.setLineWidth(0.5f);
            float hmargin = (widths[column + 1] - widths[column]) * padx;
            float barx = widths[column] + hmargin;
            float bary = (heights[0] + heights[1] - height) / 2;
            float barw = widths[column + 1] - widths[column] - (2 * hmargin);
            float barh = height;
            float coveredw = barw * coveredpc;

            if (coveredpc >= 0) {
                // uncovered
                cb.setColorFill(colours.COL_BAR_UNCOVERED);
                cb.rectangle(barx, bary, barw, barh);
                cb.fill();

                // covered
                cb.setColorFill(colours.COL_BAR_COVERED);
                cb.rectangle(barx, bary, coveredw, barh);
                cb.fill();

                //border
                cb.setColorStroke(colours.COL_BAR_BORDER);
                cb.rectangle(barx, bary, coveredw, barh);
                cb.stroke();
            } else {
                // empty
                cb.setColorFill(colours.COL_BAR_NA);
                cb.rectangle(barx, bary, barw, barh);
                cb.fill();
            }
            // outer border
            cb.setColorStroke(colours.COL_BAR_BORDER);
            cb.rectangle(barx, bary, barw, barh);
            cb.stroke();

            cb.restoreState();
        }
    }

}
