package org.openclover.core.reporters.pdf;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.registry.metrics.ProjectMetrics;
import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfBorder;
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfFontStyle;
import org.openclover.core.reporters.pdf.api.PdfTable;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfTextBuilder;
import org.openclover.runtime.util.Formatting;

import java.util.Date;

/**
 * The title bars every report opens with: the report title, the coverage timestamp or the interval
 * covered, and the block of project statistics beside it.
 *
 * <p>Reached through {@link RenderingSupport}, which is the single entry point the reporters use.
 */
final class ReportHeaderTables {

    private static PdfTable createHeaderStats(PackageMetrics metrics, PDFColours colours) {
        final String level = metrics.getType();

        final PdfTable projStats = new PdfTable(5);
        projStats.setWidths(new int[]{30, 17, 18, 20, 15});
        projStats.getDefaultStyle()
                .setBorders(PdfBorder.NONE)
                .setHorizontalAlignment(PdfAlign.Horizontal.RIGHT);
        projStats.addCell(PdfText.of(level + " stats:", RenderingSupport.BOLD_10));
        projStats.addCell(PdfText.of("LOC:", RenderingSupport.BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getLineCount()), RenderingSupport.TEXT_10));
        projStats.addCell(PdfText.of("Methods:", RenderingSupport.BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNumMethods()), RenderingSupport.TEXT_10));
        projStats.addCell();
        projStats.addCell(PdfText.of("NCLOC:", RenderingSupport.BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNcLineCount()), RenderingSupport.TEXT_10));
        projStats.addCell(PdfText.of("Classes:", RenderingSupport.BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNumClasses()), RenderingSupport.TEXT_10));
        projStats.addCell();
        projStats.addCell(PdfText.of("Files:", RenderingSupport.BOLD_10));
        projStats.addCell(PdfText.of(Formatting.formatInt(metrics.getNumFiles()), RenderingSupport.TEXT_10));
        if (metrics instanceof ProjectMetrics) {
            projStats.addCell(PdfText.of("Pkgs:", RenderingSupport.BOLD_10));
            projStats.addCell(PdfText.of(
                    Formatting.formatInt(((ProjectMetrics) metrics).getNumPackages()), RenderingSupport.TEXT_10));
        } else {
            projStats.addCell();
            projStats.addCell();
        }
        return projStats;
    }

    static PdfTable createReportHeader(HasMetrics hasMetrics, long timestamp, String title,
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

        final PdfTextBuilder titleText = PdfText.builder().add("OpenClover Coverage Report", RenderingSupport.BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);

        titleText.add("\nCoverage timestamp: ", RenderingSupport.BOLD_10);
        titleText.add(Formatting.formatDate(new Date(timestamp)), RenderingSupport.TEXT_10);

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
                target.add("\n" + title, RenderingSupport.BOLD_12);
            }
        }
    }

    static PdfTable createHistoricalPageHeader(String title, String titleAnchor, PDFColours colours) {
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

        final PdfTextBuilder titleText = PdfText.builder().add("Historical Coverage Report", RenderingSupport.BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);

        leftTab.addCell(titleText.build());
        titleBar.addCell(leftTab);
        return titleBar;
    }

    static PdfTable createHistoricalReportHeader(HasMetrics hasMetrics, long ts1, long ts2,
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

        final PdfTextBuilder titleText = PdfText.builder().add("Historical Coverage Report", RenderingSupport.BOLD_14);
        appendReportTitle(titleText, title, titleAnchor, colours);
        // the title spans both columns; the timestamps below it are label/value pairs
        leftTab.addCell(titleText.build(), RenderingSupport.FULL_WIDTH);

        leftTab.addCell(PdfText.of("From: ", RenderingSupport.BOLD_10), RenderingSupport.RIGHT_ALIGNED);
        leftTab.addCell(PdfText.of(Formatting.formatDate(new Date(ts1)), RenderingSupport.TEXT_10));
        leftTab.addCell(PdfText.of("To: ", RenderingSupport.BOLD_10), RenderingSupport.RIGHT_ALIGNED);
        leftTab.addCell(PdfText.of(Formatting.formatDate(new Date(ts2)), RenderingSupport.TEXT_10));

        titleBar.addCell(leftTab);
        titleBar.addCell(createHeaderStats((PackageMetrics) metrics, colours));
        return titleBar;
    }
    private ReportHeaderTables() {
    }
}
