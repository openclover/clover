package org.openclover.core.reporters.pdf;


import clover.com.lowagie.text.Document;
import clover.com.lowagie.text.DocumentException;
import clover.com.lowagie.text.FontFactory;
import clover.com.lowagie.text.Rectangle;
import clover.com.lowagie.text.pdf.PdfWriter;
import org.openclover.core.CloverLicenseInfo;
import org.openclover.core.CodeType;
import org.openclover.core.api.command.ArgProcessor;
import org.openclover.core.api.command.HelpBuilder;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.CloverReporter;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.Format;
import org.openclover.core.reporters.Historical;
import org.openclover.core.reporters.util.HistoricalReportDescriptor;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org_openclover_runtime.CloverVersionInfo;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.openclover.core.reporters.CommandLineArgProcessors.AlwaysReport;
import static org.openclover.core.reporters.CommandLineArgProcessors.DebugLogging;
import static org.openclover.core.reporters.CommandLineArgProcessors.Filter;
import static org.openclover.core.reporters.CommandLineArgProcessors.HideBars;
import static org.openclover.core.reporters.CommandLineArgProcessors.IncludeFailedTestCoverage;
import static org.openclover.core.reporters.CommandLineArgProcessors.InitString;
import static org.openclover.core.reporters.CommandLineArgProcessors.OrderBy;
import static org.openclover.core.reporters.CommandLineArgProcessors.OutputFile;
import static org.openclover.core.reporters.CommandLineArgProcessors.PageSize;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowEmpty;
import static org.openclover.core.reporters.CommandLineArgProcessors.Span;
import static org.openclover.core.reporters.CommandLineArgProcessors.ThreadCount;
import static org.openclover.core.reporters.CommandLineArgProcessors.Title;
import static org.openclover.core.reporters.CommandLineArgProcessors.VerboseLogging;
import static org.openclover.core.util.Lists.join;
import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Maps.newHashMap;


public class PDFReporter extends CloverReporter {

    @SuppressWarnings("unchecked")
    private static final List<ArgProcessor<Current>> mandatoryArgProcessors = newArrayList(
            InitString,
            OutputFile
    );

    @SuppressWarnings("unchecked")
    private static final List<ArgProcessor<Current>> optionalArgProcessors = newArrayList(
            AlwaysReport,
            HideBars,
            OrderBy,
            DebugLogging,
            ShowEmpty,
            Filter,
            IncludeFailedTestCoverage,
            PageSize,
            Span,
            Title,
            ThreadCount,
            VerboseLogging
    );

    private static final List<ArgProcessor<Current>> allArgProcessors =
            join(mandatoryArgProcessors, optionalArgProcessors);


    private static final Rectangle DEFAULT_PAGE_SIZE = clover.com.lowagie.text.PageSize.A4;
    private static final Map<String, Rectangle> SUPPORTED_PAGE_SIZES = newHashMap();

    static {
        SUPPORTED_PAGE_SIZES.put("A4", clover.com.lowagie.text.PageSize.A4);
        SUPPORTED_PAGE_SIZES.put("LETTER", clover.com.lowagie.text.PageSize.LETTER);
    }

    private final Document document;
    private final PDFColours colours;
    private final String reportTitle;
    private final String titleAnchor;
    private final Rectangle docsize;
    private final PdfWriter docWriter;
    private final CloverReportConfig[] secondaryConfigs;

    public PDFReporter(CloverReportConfig config) throws CloverException {
        this(config, new CloverReportConfig[] {});
    }

    public PDFReporter(CloverReportConfig config, CloverReportConfig[] secondaryConfigs) throws CloverException {
        super(config);
        try {
            this.secondaryConfigs = secondaryConfigs;
            this.reportTitle = config.getTitle();
            this.titleAnchor = (config.getTitleAnchor() != null ? config.getTitleAnchor() : "");
            this.colours = config.getFormat().getBw() ? PDFColours.BW_COLOURS : PDFColours.COL_COLOURS;

            this.docsize = getConfiguredPageSize(config);
            this.document = new Document(docsize, 25, 25, 25, 35); //##HACK - magic - bottom bigger for footer

            this.document.addTitle("Clover Coverage Report");
            this.document.addCreator("Clover " + CloverVersionInfo.RELEASE_NUM + " using iText v0.96");
            this.docWriter = PdfWriter.getInstance(document, Files.newOutputStream(config.getOutFile().toPath()));
            this.docWriter.setPageEvent(new PageFooterRenderer(docsize, System.currentTimeMillis(), colours));
        } catch (Exception e) {
            throw new CloverException("Report rendering error: " + e.getMessage());
        }
    }

    @Override
    protected int executeImpl() throws CloverException {
        open();
        boolean written = write(reportConfig);
        for (CloverReportConfig secondaryConfig : secondaryConfigs) {
            write(secondaryConfig);
        }
        if (written) {
            close();
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    protected void validate() throws CloverException {
        super.validate();
        for (CloverReportConfig secondaryConfig : secondaryConfigs) {
            if (!secondaryConfig.validate()) {
                throw new CloverException(secondaryConfig.getValidationFailureReason());
            }
        }
    }

    private void open() {
        document.open();
    }

    private void close() {
        document.close();
    }

    private Rectangle getConfiguredPageSize(CloverReportConfig cfg) {
        Rectangle size;
        final String sizeStr = cfg.getFormat().getPageSize();
        if (sizeStr != null) {
            size = SUPPORTED_PAGE_SIZES.get(sizeStr);
            if (size == null) {
                Logger.getInstance().warn("Unsupported Page Size '" + sizeStr
                        + "', using default.");
                size = DEFAULT_PAGE_SIZE;
            }
        } else {
            size = DEFAULT_PAGE_SIZE;
        }
        return size;
    }

    private boolean write(CloverReportConfig config) throws CloverException {
        try {
            if (config instanceof Current) {
                // do an initial check to see if we should bother generating a pdf at all
                if (!config.isAlwaysReport() && !config.getCoverageDatabase().hasCoverage()) {
                    Logger.getInstance().warn("No coverage recordings found. No report will be generated.");
                    return false;
                }
                generateCurrentReport((Current)config);
            } else {
                HistoricalReportDescriptor desc = new HistoricalReportDescriptor(config);
                final boolean hasHistoricalData = desc.gatherHistoricalModels();

                if (!hasHistoricalData) {
                    Logger.getInstance().warn("No historical data found. No PDF historical report can be generated.");
                    return false;
                }
                generateHistoricalReport((Historical)config, desc);
            }
        }
        catch (Exception e) {
            throw new CloverException("Report rendering error: " + e.getMessage(), e);
        }
        return true;
    }

    private void newPage() throws DocumentException {
        document.newPage();
        document.add(RenderingSupport.createHistoricalPageHeader(reportTitle, titleAnchor, colours));
        if (CloverLicenseInfo.EXPIRED) {
            document.add(RenderingSupport.createLicenseWarningBar(FontFactory.HELVETICA, 10, colours));
        } else {
            document.add(RenderingSupport.getSpacerRow());
        }
    }

    private void generateHistoricalReport(Historical historicalConfig, HistoricalReportDescriptor desc) throws DocumentException {
        document.add(
            RenderingSupport.createHistoricalReportHeader(
                desc.getSubjectMetrics(), desc.getFirstTimestamp(), desc.getLastTimestamp(),
                reportTitle, titleAnchor, !desc.isPackageLevel(), colours));

        if (CloverLicenseInfo.EXPIRED) {
            document.add(RenderingSupport.createLicenseWarningBar(FontFactory.HELVETICA, 10, colours));
        } else {
            document.add(RenderingSupport.getSpacerRow());
        }

        if (desc.showOverview()) {
            List<HasMetrics> parentItem = newLinkedList();
            parentItem.add(desc.getSubjectMetrics());
            document.add(RenderingSupport.createCoverageDataTable(historicalConfig, desc.getSubjectName(), parentItem, colours));
            document.add(RenderingSupport.getSpacerRow());
        }
        int chartsOnPage = 0;

        List<Historical.Chart> charts = historicalConfig.getCharts();
        
        Map<Long, HasMetrics> data = desc.getHistoricalModels();

        for (Object chart : charts) {
            if (chartsOnPage == 2) {
                newPage();
                chartsOnPage = 0;
            }
            final Historical.Chart coverage = (Historical.Chart) chart;
            coverage.setHeight((int) (0.33f * docsize.height()));
            document.add(RenderingSupport.createChart(coverage, data, colours));
            document.add(RenderingSupport.getSpacerRow());
            chartsOnPage++;
        }

        if (desc.showMovers()) {
            if (chartsOnPage == 2) {
                newPage();
                chartsOnPage = 0;
            }
            for (Iterator<HistoricalReportDescriptor.AddedDescriptor> iter =
                 desc.getAddedDescriptors().iterator(); iter.hasNext();) {
                document.add(
                    RenderingSupport.createAddedTable(iter.next(), colours));
                if (iter.hasNext() || !desc.getMoversDescriptors().isEmpty()) {
                    document.add(RenderingSupport.getSpacerRow());
                }
            }
            for (Iterator<HistoricalReportDescriptor.MoversDescriptor> iter =
                 desc.getMoversDescriptors().iterator(); iter.hasNext();) {
                document.add(
                    RenderingSupport.createMoversTable(iter.next(), colours));
                if (iter.hasNext()) {
                    document.add(RenderingSupport.getSpacerRow());
                }
            }
        }
        document.newPage();
    }

    private void generateCurrentReport(Current currentConfig) throws DocumentException {
        final ProjectInfo project = database.getModel(CodeType.APPLICATION);

        HasMetrics parent;
        String parentTitle;
        String childrenTitle;
        Logger.getInstance().debug("creating project summary report");
        parent = project;

        List<PackageInfo> children = project.getAllPackages();
        Logger.getInstance().debug("num packages = " + children.size());
        parentTitle = "";
        childrenTitle = "Packages";
        document.add(
            RenderingSupport.createReportHeader(
                project, database.getRecordingTimestamp(), reportTitle, titleAnchor, colours));

        if (CloverLicenseInfo.EXPIRED) {
            document.add(RenderingSupport.createLicenseWarningBar(FontFactory.HELVETICA, 10, colours));
        } else {
            document.add(RenderingSupport.getSpacerRow());
        }

        document.add(RenderingSupport.createCoverageDataTable(currentConfig, parentTitle, Collections.singletonList(parent), colours));
        document.add(RenderingSupport.getSpacerRow());

        children.sort(HasMetricsSupport.getHasMetricsComparator(currentConfig.getFormat().getOrderby()));

        document.add(RenderingSupport.createCoverageDataTable(currentConfig, childrenTitle, children, colours));
        document.newPage();
    }

    public static void main(String[] args) {
        loadLicense();
        System.exit(runReport(args));
    }

    public static int runReport(String[] args) {
        CloverReportConfig config = processArgs(args);
        
        if (canProceedWithReporting(config)) {
            try {
                return new PDFReporter(config, new CloverReportConfig[]{}).execute();
            } catch (Exception e) {
                Logger.getInstance().error("A problem was encountered while rendering the report: " + e.getMessage(), e);
            }
        }
        return 1;
    }

    private static CloverReportConfig processArgs(String[] args) {
        Current config = new Current(Current.DEFAULT_PDF);
        config.setFormat(Format.DEFAULT_PDF);
        try {
            int i = 0;

            while (i < args.length) {
                for (ArgProcessor<Current> argProcessor : allArgProcessors) {
                    if (argProcessor.matches(args, i)) {
                        i = argProcessor.process(args, i, config);
                    }
                }
                i++;
            }

            if (!config.validate()) {
                usage(config.getValidationFailureReason());
                config = null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage("Missing a parameter.");
            config = null;
        }
        return config;
    }

    private static void usage(String msg) {
        System.err.println();
        if (msg != null) {
            System.err.println("  *** ERROR: "+msg);
        }
        System.err.println();

        System.err.println(HelpBuilder.buildHelp(PDFReporter.class, mandatoryArgProcessors, optionalArgProcessors));

        System.err.println();
    }
}
