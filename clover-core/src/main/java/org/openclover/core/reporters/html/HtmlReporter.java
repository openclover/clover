package org.openclover.core.reporters.html;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.command.ArgProcessor;
import org.openclover.core.api.command.HelpBuilder;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.PackageFragment;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.cfg.Interval;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.CloverReporter;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.Format;
import org.openclover.core.reporters.Historical;
import org.openclover.core.reporters.TestSelectionHelper;
import org.openclover.core.reporters.Type;
import org.openclover.core.reporters.filters.SourceFileFilter;
import org.openclover.core.reporters.json.JSONHistoricalReporter;
import org.openclover.core.reporters.json.RenderMetricsJSONAction;
import org.openclover.core.reporters.json.RenderTreeMapAction;
import org.openclover.core.reporters.util.CloverChartFactory;
import org.openclover.core.reporters.util.HistoricalReportDescriptor;
import org.openclover.core.util.CloverExecutor;
import org.openclover.core.util.CloverExecutors;
import org.openclover.core.util.CloverUtils;
import org.openclover.core.util.FileUtils;
import org.openclover.core.util.format.HtmlFormatter;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org_openclover_runtime.CloverVersionInfo;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.openclover.core.reporters.CommandLineArgProcessors.AlwaysReport;
import static org.openclover.core.reporters.CommandLineArgProcessors.BlackAndWhite;
import static org.openclover.core.reporters.CommandLineArgProcessors.DebugLogging;
import static org.openclover.core.reporters.CommandLineArgProcessors.Filter;
import static org.openclover.core.reporters.CommandLineArgProcessors.HideBars;
import static org.openclover.core.reporters.CommandLineArgProcessors.HideSources;
import static org.openclover.core.reporters.CommandLineArgProcessors.IncludeFailedTestCoverage;
import static org.openclover.core.reporters.CommandLineArgProcessors.InitString;
import static org.openclover.core.reporters.CommandLineArgProcessors.NoCache;
import static org.openclover.core.reporters.CommandLineArgProcessors.OrderBy;
import static org.openclover.core.reporters.CommandLineArgProcessors.OutputDirHtml;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowEmpty;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowInnerFunctions;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowLambdaFunctions;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowUnique;
import static org.openclover.core.reporters.CommandLineArgProcessors.SourcePath;
import static org.openclover.core.reporters.CommandLineArgProcessors.Span;
import static org.openclover.core.reporters.CommandLineArgProcessors.TabWidth;
import static org.openclover.core.reporters.CommandLineArgProcessors.ThreadCount;
import static org.openclover.core.reporters.CommandLineArgProcessors.Title;
import static org.openclover.core.reporters.CommandLineArgProcessors.VerboseLogging;
import static org.openclover.core.util.Lists.join;
import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Maps.newLinkedHashMap;

public class HtmlReporter extends CloverReporter {

    private static final List<ArgProcessor<Current>> mandatoryArgProcessors = newArrayList(
            InitString,
            OutputDirHtml
    );

    private static final List<ArgProcessor<Current>> optionalArgProcessors = newArrayList(
            AlwaysReport,
            HideBars,
            BlackAndWhite,
            OrderBy,
            DebugLogging,
            ShowEmpty,
            Filter,
            HideSources,
            IncludeFailedTestCoverage,
            NoCache,
            SourcePath,
            Span,
            ShowInnerFunctions,
            ShowLambdaFunctions,
            ShowUnique,
            Title,
            ThreadCount,
            TabWidth,
            VerboseLogging
    );

    private static final List<ArgProcessor<Current>> allArgProcessors =
            join(mandatoryArgProcessors, optionalArgProcessors);

    /**
     * Map of valid "homepage" values
     */
    private static final Map<String, String> HTML_HOMEPAGE_VALUES = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("overview", "pkg-summary.html");
        put("aggregate", "agg-pkgs.html");
        put("dashboard", "dashboard.html");
        put("quickwins", "quick-wins.html");
        put("projectrisks", "proj-risks.html");
        put("testresults", "test-pkg-summary.html");
    }});

    /**
     * The default homepage to use if none configured
     **/
    private static final String HTML_HOMEPAGE_DEFAULT = "dashboard";

    /**
     * The summary Tabs for the bottom left frame
     **/
    protected static final Map<String, String> SUMMARY_TABS = Collections.unmodifiableMap(new LinkedHashMap<String, String>() {{
        put(TAB_CLASSES, "pkg-app.html");
        put(TAB_TESTS, "pkg-test.html");
        put(TAB_RESULTS, "pkg-results.html");
    }});

    protected static final String TAB_CLASSES = "Classes";
    protected static final String TAB_TESTS = "Tests";
    protected static final String TAB_RESULTS = "Results";

    private static final Comparator<HasMetrics> TEST_SORT_ORDER = HasMetricsSupport.newTestListComparator();

    private static final Comparator<TestCaseInfo> TEST_CASE_COMPARATOR = (lhs, rhs) -> {
        if (rhs.isSuccess() & lhs.isSuccess()) {
            return 0;
        } else if (!rhs.isSuccess()) {
            return 1;
        } else {
            return -1;
        }
    };

    private final DateFormat dateFormat = new SimpleDateFormat("EEE MMM d yyyy HH:mm:ss z");
    private final File basePath;
    private final File baseImagePath;
    private final HtmlRenderingSupportImpl rederingHelper;
    private final String reportTimeStamp;
    private final Comparator<HasMetrics> listComparator;
    private final String pageTitle;
    private final String pageTitleAnchor;
    private final String pageTitleTarget;
    private Date coverageTS;
    private Comparator<HasMetrics> detailComparator;

    public HtmlReporter(CloverReportConfig config) throws CloverException {
        super(config);
        rederingHelper = new HtmlRenderingSupportImpl(this.reportConfig.getFormat(), true); // TODO: detect a model with a filter
        basePath = this.reportConfig.getOutFile();
        baseImagePath = new File(basePath, "img");
        reportTimeStamp = dateFormat.format(new Date(System.currentTimeMillis()));
        pageTitleAnchor = (config.getTitleAnchor() != null ? config.getTitleAnchor() : "");
        pageTitleTarget = (config.getTitleTarget() != null ? config.getTitleTarget() : "_top");
        pageTitle = config.getTitle();
        final String comp = config.getFormat().getOrderby();
        listComparator = HasMetricsSupport.LEX_COMP;
        detailComparator = HasMetricsSupport.PC_ASCENDING_COMP;
        if (comp != null) {// ##HACK - this should be encapsulated in config.
            // now look for the new comparator names
            detailComparator = HasMetricsSupport.getHasMetricsComparator(comp);
        }
    }

    @Override
    protected void validate() throws CloverException {
        super.validate();
        if (!isCurrentReport() && !isHistoricalReport()) {
            throw new CloverException("Unsupported report type: " + reportConfig.getClass().getName());
        }
    }

    @Override
    protected int executeImpl() throws CloverException {
        try {
            CloverUtils.createDir(basePath);
            CloverUtils.createDir(baseImagePath);

            if (isCurrentReport()) {
                executeCurrentReport();
            } else if (isHistoricalReport()) {
                executeHistoricalReport();
            } else {
                throw new CloverException("No report type specified");
            }
            return 0;
        } catch (Exception e) {
            throw new CloverException(e);
        }
    }

    /**
     * A method which removes any reports which are not "readable by humans" (HTML or PDF or TEXT).
     */
    private void filterLinkedReports() {
        final Map<String, CloverReportConfig> filteredLinkedReports = newLinkedHashMap();
        for (Map.Entry<String, CloverReportConfig> linkedReport : reportConfig.getLinkedReports().entrySet()) {
            final CloverReportConfig linkedConfig = linkedReport.getValue();
            if (!linkedConfig.validate()) {
                Logger.getInstance().warn("Not linking report due to: " + linkedConfig.getValidationFailureReason());
                continue;
            }
            final Format format = linkedConfig.getFormat();
            if (format.in(Type.HTML, Type.PDF, Type.TEXT)) {
                filteredLinkedReports.put(linkedReport.getKey(), linkedConfig);
            }
        }
        reportConfig.setLinkedReports(filteredLinkedReports);
    }

    private void executeCurrentReport() throws Exception {
        if (!reportConfig.isAlwaysReport() && !database.hasCoverage()) {
            Logger.getInstance().warn("No coverage recordings found. No report will be generated.");
        } else {
            Logger.getInstance().info("Writing HTML report to '" + basePath + "'");
            coverageTS = new Date(database.getRecordingTimestamp());
            // remove any non-linkable reports
            filterLinkedReports();

            final long currentStartTime = System.currentTimeMillis();

            List<PackageInfo> allPackages = getFullModel().getAllPackages();
            getFullModel().buildCaches();

            TreeInfo appSrcTree = new TreeInfo("", "App");
            TreeInfo appCloudTree = new TreeInfo("", "AppCloud");
            TreeInfo testSrcTree = new TreeInfo("testsrc-", "Test");

            try {
                List<ClassInfo> targetClasses = getConfiguredModel().getClasses(HasMetricsFilter.ACCEPT_ALL);
                List<ClassInfo> testClasses = getTestModel().getClasses(HasMetricsFilter.ACCEPT_ALL);
                List<FileInfo> targetFiles = getFullModel().getFiles(new SourceFileFilter());

                final Map<Integer, CloverChartFactory.ChartInfo> srcFileCharts =
                        CloverChartFactory.generateSrcFileCharts(targetFiles, baseImagePath);

                final CloverExecutor<Object> service = CloverExecutors.newCloverExecutor(
                        reportAsCurrent().getNumThreads(), "OpenClover");
                RenderFileAction.initThreadLocals();
                RenderMetricsJSONAction.initThreadLocals();
                for (PackageInfo pkg : allPackages) {

                    Logger.getInstance().verbose("Processing package " + pkg.getName());
                    long start = System.currentTimeMillis();
                    processPackage(pkg, appSrcTree, appCloudTree, testSrcTree, service, srcFileCharts);
                    long total = System.currentTimeMillis() - start;
                    if (Logger.isDebug()) {
                        Logger.getInstance().debug(
                                "Processed package: " + pkg.getName() +
                                        " (" + pkg.getClasses().size() + " classes, " +
                                        pkg.getMetrics().getNumTests() + " tests)" +
                                        " in " + total + "ms");
                    }
                }

                renderPackageNodesTree(service);
                renderDashboard(service,
                        CloverChartFactory.generateHistogramChart(targetClasses, baseImagePath),
                        CloverChartFactory.generateScatterChart(targetClasses, baseImagePath));
                renderProjectCoverageCloudPage(appCloudTree, service);
                renderProjectTreeMapPage(service);
                renderBasePages();
                renderTestResultsPkgsSummaryPage();
                renderAggregatePkgPage(getConfiguredModel(), appSrcTree, true);
                renderPackagesSummaryPage(getConfiguredModel(), appSrcTree, true);
                renderAggregatePkgPage(getTestModel(), testSrcTree, false);
                renderPackagesSummaryPage(getTestModel(), testSrcTree, false);

                copyCommonResources(); // copy png, css, js etc

                service.shutdown();
                Interval timeOut = reportAsCurrent().getTimeOut();
                if (!service.awaitTermination(timeOut.getValueInMillis(), TimeUnit.MILLISECONDS)) {
                    throw new CloverException("Timeout of '" + timeOut + "' reached during report generation. " +
                            "Please increase this value and try again.");
                }
            } finally {
                RenderFileAction.resetThreadLocals();
                RenderMetricsJSONAction.resetThreadLocals();
            }

            final long currentTotalTime = System.currentTimeMillis() - currentStartTime;
            final int pkgCount = allPackages.size();
            final long msPerPkg = pkgCount == 0 ? currentTotalTime : currentTotalTime / pkgCount;
            Logger.getInstance().info("Done. Processed " + pkgCount + " packages in " + currentTotalTime +
                    "ms (" + msPerPkg + "ms per package).");
        }
    }

    private void executeHistoricalReport() throws Exception {
        Logger.getInstance().info("Writing historical report to '" + basePath + "'");
        final HistoricalReportDescriptor descriptor = new HistoricalReportDescriptor(reportConfig);
        final boolean hasHistoricalData = descriptor.gatherHistoricalModels();

        if (!hasHistoricalData) {
            Logger.getInstance().warn("No historical data found. No HTML historical report can be generated.");
            return;
        }

        coverageTS = new Date(descriptor.getFirstTimestamp());

        filterLinkedReports();
        final VelocityContext context = new VelocityContext();
        insertCommonPropsForHistorical(context, "");

        HtmlReportUtil.mergeTemplateToDir(basePath, "style.css", context);

        final File outfile = new File(basePath, reportConfig.getMainFileName());
        context.put("historical", descriptor);
        final CloverReportConfig firstCurrentConfig = reportConfig.getFirstCurrentConfig();
        if (firstCurrentConfig != null) {
            String relToCurrentRoot = FileUtils.getRelativePath(outfile.getParentFile(), firstCurrentConfig.getMainOutFile().getParentFile(), "/");
            relToCurrentRoot = "".equals(relToCurrentRoot) ? "" : relToCurrentRoot + "/";
            context.put("relToCurrentRoot", relToCurrentRoot);
            Format format = firstCurrentConfig.getFormat();
            if (format != null) {
                context.put("showSrc", format.getSrcLevel());
            }
        }

        context.put("hasmetrics", descriptor.getSubjectMetrics());
        context.put("endTimestamp", dateFormat.format(new Date(descriptor.getLastTimestamp())));
        if (descriptor.showMovers()) {
            context.put("allAdded", descriptor.getAddedDescriptors());
            context.put("allMovers", descriptor.getMoversDescriptors());
        }

        context.put("colSpan", 6);

        copyCommonResources(); // copy png, css, js etc
        final File imgDir = createChartImageDir(); // create 'img' dir for charts

        final Historical historical = (Historical) reportConfig;
        final List<Historical.Chart> charts = historical.getCharts();

        final Map<Long, HasMetrics> data = descriptor.getHistoricalModels();
        final List<String> chartNames = newArrayList();

        final Map<String, String> imageMaps = newHashMap();
        for (int i = 0; i < charts.size(); ++i) {
            String chartName = "chart" + i + ".jpg";
            chartNames.add(chartName);
            Historical.Chart chart = charts.get(i);

            final JFreeChart jFreeChart = CloverChartFactory.createJFreeChart(chart, data);
            final ChartRenderingInfo renderingInfo = new ChartRenderingInfo();
            ChartUtilities.saveChartAsJPEG(new File(imgDir, chartName), 1.0f, jFreeChart,
                    chart.getWidth(), chart.getHeight(), renderingInfo);
            final String imageMap = ChartUtilities.getImageMap(chartName, renderingInfo);
            imageMaps.put(chartName, imageMap);
        }
        context.put("imageMaps", imageMaps);
        context.put("chartNames", chartNames);

        HtmlReportUtil.mergeTemplateToFile(outfile, context, "historical.vm");

        if (historical.isJson()) {
            final JSONHistoricalReporter jsonReporter = new JSONHistoricalReporter(
                    reportConfig.getOutFile());
            jsonReporter.generateHistoricalJSON(context, data, pageTitle);
        }

        Logger.getInstance().info("Done.");
    }

    private Current reportAsCurrent() {
        return ((Current) reportConfig);
    }

    static Current processArgs(String[] args) {
        final Current cfg = new Current();
        cfg.setFormat(Format.DEFAULT_HTML);
        try {
            int i = 0;
            while (i < args.length) {
                for (ArgProcessor<Current> argProcessor : allArgProcessors) {
                    if (argProcessor.matches(args, i)) {
                        i = argProcessor.process(args, i, cfg);
                    }
                }
                i++;
            }

            TestSelectionHelper.configureTestSelectionFilter(cfg, args);

            if (!cfg.validate()) {
                usage(cfg.getValidationFailureReason());
                return null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage("Missing a parameter.");
            return null;
        }
        return cfg;
    }

    private static void usage(String msg) {
        System.err.println();
        if (msg != null) {
            System.err.println("  *** ERROR: " + msg);
        }
        System.err.println();

        System.err.println(HelpBuilder.buildHelp(HtmlReporter.class, mandatoryArgProcessors, optionalArgProcessors));
        System.err.println(TestSelectionHelper.getParamsUsage());

        System.err.println();
    }

    public static void main(String[] args) {
        System.exit(runReport(args));
    }

    public static int runReport(String[] args) {
        Current cfg = processArgs(args);
        if (canProceedWithReporting(cfg)) {
            try {
                return new HtmlReporter(cfg).execute();
            } catch (Exception e) {
                Logger.getInstance().error("A problem was encountered while rendering the report: " + e.getMessage(), e);
            }
        }
        return 1;
    }

    private VelocityContext insertCommonPropsForCurrent(VelocityContext context, String pkg) {
        return insertCommonProps(context, pkg);
    }

    private VelocityContext insertCommonPropsForHistorical(VelocityContext context, String pkg) {
        return insertCommonProps(context, pkg);
    }

    private VelocityContext insertCommonProps(VelocityContext context, String pkg) {
        context.put("fileUtils", FileUtils.getInstance());
        context.put("stringUtils", new StringUtils());

        context.put("rootRelPath", rederingHelper.getRootRelPath(pkg));
        context.put("pageTitle", pageTitle);
        String title = (pageTitle != null ? pageTitle : "OpenClover");
        context.put("headerTitle", pkg.length() == 0 ? title : title + ": " + pkg);
        context.put("pageTitleIsLink",
                pageTitleAnchor != null
                        && pageTitleAnchor.length() > 0);
        context.put("pageTitleAnchor", pageTitleAnchor);
        context.put("pageTitleTarget", pageTitleTarget);
        context.put("renderUtil", rederingHelper);
        context.put("startTimestamp", dateFormat.format(coverageTS));

        String cloverURL = CloverVersionInfo.OPENCLOVER_ORG;
        context.put("cloverURL", cloverURL);
        context.put("cloverReleaseNum", CloverVersionInfo.RELEASE_NUM);
        context.put("reportTimestamp", reportTimeStamp);
        context.put("showEmpty", reportConfig.getFormat().getShowEmpty());
        context.put("showSrc", reportConfig.getFormat().getSrcLevel());
        context.put("showBars", reportConfig.getFormat().getShowBars());
        context.put("noCache", reportConfig.getFormat().getNoCache());
        context.put("charset", reportConfig.getCharset());
        context.put("skipCoverageTreeMap", reportConfig.isSkipCoverageTreeMap());

        // list of linked reports (for vertical navigation / bottom-left frame)
        context.put("reportConfigLinkedReports", reportConfig.getLinkedReports());
        context.put("reportConfigOutFile", reportConfig.getOutFile());

        return context;
    }

    private void renderProjectCoverageCloudPage(TreeInfo appCloudTree, CloverExecutor<Object> service) throws Exception {
        VelocityContext cloudsContext = new VelocityContext();
        insertCommonPropsForCurrent(cloudsContext, "");
        service.submit(new RenderProjectCoverageCloudsAction(cloudsContext, reportConfig, basePath, appCloudTree, getConfiguredModel()));
    }

    private void renderProjectTreeMapPage(CloverExecutor<Object> service) throws Exception {
        VelocityContext context = new VelocityContext();
        insertCommonPropsForCurrent(context, "");
        service.submit(new RenderTreeMapAction(context, reportConfig, basePath, getConfiguredModel()));
    }

    protected ProjectInfo getConfiguredModel() {
        return database.getAppOnlyModel();
    }

    protected ProjectInfo getFullModel() {
        return database.getFullModel();
    }

    protected ProjectInfo getTestModel() {
        return database.getTestOnlyModel();
    }

    private void renderPackageNodesTree(CloverExecutor<Object> queue) throws Exception {
        VelocityContext ctx = new VelocityContext();
        insertCommonPropsForCurrent(ctx, "");
        RenderPackageTreeJsonAction action = new RenderPackageTreeJsonAction(ctx, basePath,
                getFullModel(), getConfiguredModel(), reportAsCurrent());
        queue.submit(action);
    }

    private void renderDashboard(CloverExecutor<Object> queue, CloverChartFactory.ChartInfo histogram, CloverChartFactory.ChartInfo scatter) throws Exception {
        VelocityContext ctx = new VelocityContext();
        insertCommonPropsForCurrent(ctx, "");
        final ProjectInfo configuredProject = getConfiguredModel();
        RenderDashboardAction action = new RenderDashboardAction(ctx, basePath, configuredProject, getFullModel(),
                histogram, scatter, reportAsCurrent());
        queue.submit(action);
        final File outfile = new File(reportAsCurrent().getOutFile(), "project.js");
        RenderMetricsJSONAction jsonAction =
                new RenderMetricsJSONAction(ctx, configuredProject, reportAsCurrent(), outfile, rederingHelper);
        queue.submit(jsonAction);
    }

    private File createChartImageDir() {
        final File imgDir = new File(basePath, "img");
        imgDir.mkdir();
        return imgDir;
    }

    /**
     * Copy resources for ADG report
     */
    private void copyCommonResources() throws IOException {
        final String templatePath = HtmlReportUtil.getTemplatePath();

        // Clover-specific icons and javascripts
        copyCommonResourcesBoth(templatePath);

        // AUI files
        copyStaticResource(templatePath, "aui/css/arrow.png");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.eot");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.svg");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.ttf");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.woff");
        copyStaticResource(templatePath, "aui/css/aui.min.css");
        copyStaticResource(templatePath, "aui/css/aui-experimental.min.css");
        copyStaticResource(templatePath, "aui/css/aui-icon-close.png");
        copyStaticResource(templatePath, "aui/css/aui-icon-tools.gif");
        copyStaticResource(templatePath, "aui/css/aui-ie9.min.css");
        copyStaticResource(templatePath, "aui/css/aui-toolbar-24px.png");
        copyStaticResource(templatePath, "aui/css/bg-000-trans20.png");
        copyStaticResource(templatePath, "aui/css/bg-000-trans50.png");
        copyStaticResource(templatePath, "aui/css/bg-grippy.png");
        copyStaticResource(templatePath, "aui/css/core/icon-dropdown.png");
        copyStaticResource(templatePath, "aui/css/core/icon-dropdown-active.png");
        copyStaticResource(templatePath, "aui/css/core/icon-dropdown-active-d.png");
        copyStaticResource(templatePath, "aui/css/core/icon-dropdown-d.png");
        copyStaticResource(templatePath, "aui/css/core/icon-maximize.png");
        copyStaticResource(templatePath, "aui/css/core/icon-maximize-d.png");
        copyStaticResource(templatePath, "aui/css/core/icon-minimize.png");
        copyStaticResource(templatePath, "aui/css/core/icon-minimize-d.png");
        copyStaticResource(templatePath, "aui/css/core/icon-move.png");
        copyStaticResource(templatePath, "aui/css/core/icon-move-d.png");
        copyStaticResource(templatePath, "aui/css/core/icon-search.png");
        copyStaticResource(templatePath, "aui/css/fav_off_16.png");
        copyStaticResource(templatePath, "aui/css/fav_on_16.png");
        copyStaticResource(templatePath, "aui/css/fonts/atlassian-icons.eot");
        copyStaticResource(templatePath, "aui/css/fonts/atlassian-icons.svg");
        copyStaticResource(templatePath, "aui/css/fonts/atlassian-icons.ttf");
        copyStaticResource(templatePath, "aui/css/fonts/atlassian-icons.woff");
        copyStaticResource(templatePath, "aui/css/forms/icon-date.png");
        copyStaticResource(templatePath, "aui/css/forms/icon-help.png");
        copyStaticResource(templatePath, "aui/css/forms/icon-range.png");
        copyStaticResource(templatePath, "aui/css/forms/icon-required.png");
        copyStaticResource(templatePath, "aui/css/forms/icons_form.gif");
        copyStaticResource(templatePath, "aui/css/forms/icon-users.png");
        copyStaticResource(templatePath, "aui/css/icons/aui-icon-close.png");
        copyStaticResource(templatePath, "aui/css/icons/aui-icon-tools.gif");
        copyStaticResource(templatePath, "aui/css/icons/aui-message-icon-sprite.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-dropdown.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-dropdown-active.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-dropdown-active-d.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-dropdown-d.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-maximize.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-maximize-d.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-minimize.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-minimize-d.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-move.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-move-d.png");
        copyStaticResource(templatePath, "aui/css/icons/core/icon-search.png");
        copyStaticResource(templatePath, "aui/css/icons/forms/icon-date.png");
        copyStaticResource(templatePath, "aui/css/icons/forms/icon-help.png");
        copyStaticResource(templatePath, "aui/css/icons/forms/icon-range.png");
        copyStaticResource(templatePath, "aui/css/icons/forms/icon-required.png");
        copyStaticResource(templatePath, "aui/css/icons/forms/icon-users.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-close.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-close-inverted.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-error.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-error-white.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-generic.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-hint.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-info.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-success.png");
        copyStaticResource(templatePath, "aui/css/icons/messages/icon-warning.png");
        copyStaticResource(templatePath, "aui/css/images/arrow.png");
        copyStaticResource(templatePath, "aui/css/images/bg-000-trans20.png");
        copyStaticResource(templatePath, "aui/css/images/bg-000-trans50.png");
        copyStaticResource(templatePath, "aui/css/images/fav_off_16.png");
        copyStaticResource(templatePath, "aui/css/images/fav_on_16.png");
        copyStaticResource(templatePath, "aui/css/images/forms/icons_form.gif");
        copyStaticResource(templatePath, "aui/css/images/icons/aui-message-icon-sprite.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-close.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-close-inverted.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-error.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-error-white.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-generic.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-hint.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-info.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-success.png");
        copyStaticResource(templatePath, "aui/css/images/icons/messages/icon-warning.png");
        copyStaticResource(templatePath, "aui/css/images/wait.gif");
        copyStaticResource(templatePath, "aui/css/messages/icon-close.png");
        copyStaticResource(templatePath, "aui/css/messages/icon-close-inverted.png");
        copyStaticResource(templatePath, "aui/css/select2.png");
        copyStaticResource(templatePath, "aui/css/select2-spinner.gif");
        copyStaticResource(templatePath, "aui/css/select2x2.png");
        copyStaticResource(templatePath, "aui/css/toolbar/aui-toolbar-24px.png");
        copyStaticResource(templatePath, "aui/css/wait.gif");

        copyStaticResource(templatePath, "aui/js/aui.min.js");
        copyStaticResource(templatePath, "aui/js/aui-experimental.min.js");
        copyStaticResource(templatePath, "aui/js/aui-soy.min.js");

        copyStaticResource(templatePath, "jquery-1.8.3.min.js");
        copyStaticResource(templatePath, "clover-tree.js");
        copyStaticResource(templatePath, "clover-descriptions.js");
    }

    /**
     * Copy resources common for ADG and Classic reports
     */
    private void copyCommonResourcesBoth(String templatePath) throws IOException {
        copyStaticResource(templatePath, "img/ajax-loader.gif");
        copyStaticResource(templatePath, "img/back.gif");
        copyStaticResource(templatePath, "img/clover.ico");
        copyStaticResource(templatePath, "img/openclover_logo_white_40pt_alpha_with_icon.png");
        copyStaticResource(templatePath, "img/collapse.gif");
        copyStaticResource(templatePath, "img/expand.gif");
        copyStaticResource(templatePath, "img/failure_gutter.gif");
        copyStaticResource(templatePath, "img/spacer.gif");
        copyStaticResource(templatePath, "cloud.js");
        copyStaticResource(templatePath, "clover.js");
        copyStaticResource(templatePath, "jit.js");
    }

    private void copyStaticResource(final String aLoadPath, final String aName) throws IOException {
        final File outfile = new File(basePath, aName);
        FileUtils.resourceToFile(getClass().getClassLoader(), aLoadPath + "/" + aName, outfile);
    }

    private void processPackage(final PackageInfo pkg, final TreeInfo appSrcTree, final TreeInfo appCloudTree,
                                final TreeInfo testSrcTree, final CloverExecutor<Object> queue,
                                final Map<Integer, CloverChartFactory.ChartInfo> charts) throws Exception {
        final ProjectInfo projectInfo = getFullModel();

        for (FileInfo fileInfo : pkg.getFiles()) {
            renderSourceFilePage(queue, charts, projectInfo, fileInfo);
            renderTestPages(queue, fileInfo);
        }

        PackageInfo pkgAppInfo = getConfiguredModel().getNamedPackage(pkg.getName());
        PackageInfo pkgTestInfo = getTestModel().getNamedPackage(pkg.getName());

        List<ClassInfo> testClasses = pkgTestInfo != null ? pkgTestInfo.getClasses() : new LinkedList<>();

        if (pkgAppInfo != null) {
            renderPkgSummaryPage(pkgAppInfo, appSrcTree, true, pkgTestInfo != null, true, queue);
            renderPkgCloudPages(pkgAppInfo, appCloudTree, true, pkgTestInfo != null, queue);
            renderPkgTreeMapPage(pkgAppInfo, queue);
        }

        if (pkgTestInfo != null) {
            renderPkgSummaryPage(pkgTestInfo, testSrcTree, pkgAppInfo != null, true, false, queue);
        }
        renderTestResultsPkgSummaryPages(pkg, testClasses);
    }

    private void renderSourceFilePage(final CloverExecutor<Object> queue,
                                      final Map<Integer, CloverChartFactory.ChartInfo> charts,
                                      final ProjectInfo projectInfo, FileInfo file) throws Exception {
        if (reportConfig.getFormat().getSrcLevel()) {
            queue.submit(
                    new RenderFileAction(
                            file,
                            rederingHelper,
                            reportAsCurrent(),
                            insertCommonPropsForCurrent(new VelocityContext(), file.getContainingPackage().getName()),
                            database,
                            projectInfo,
                            charts));
        }
    }

    private void renderTestPages(CloverExecutor<Object> queue, FileInfo fileInfo) throws Exception {
        List<ClassInfo> classes = fileInfo.getClasses();
        for (ClassInfo classInfo : classes) {

            if (!classInfo.isTestClass()) {
                continue;
            }
            for (TestCaseInfo test : classInfo.getTestCases()) {
                VelocityContext context = new VelocityContext();
                insertCommonPropsForCurrent(context, fileInfo.getContainingPackage().getName());
                Callable<Object> testResultRenderer =
                        new RenderTestResultAction(
                                test, rederingHelper, (Current) reportConfig,
                                getConfiguredModel(), context, getFullModel(), database);
                queue.submit(testResultRenderer);
            }
        }
    }

    private void gatherAggregatePackages(Map<String, PackageFragment> pkgs, PackageFragment frag) {
        pkgs.put(frag.getQualifiedName(), frag);
        PackageFragment[] kids = frag.getChildren();
        for (int i = 0; kids != null && i < kids.length; i++) {
            PackageFragment kid = kids[i];
            gatherAggregatePackages(pkgs, kid);
        }
    }

    private void renderAggregatePkgPage(ProjectInfo model, TreeInfo tree, boolean linkToClouds) throws Exception {
        final String filename = tree.getPathPrefix() + "agg-pkgs.html";

        final File outfile = new File(basePath, filename);
        final VelocityContext context = new VelocityContext();
        context.put("linkToClouds", linkToClouds);
        context.put("currentPageURL", filename);
        context.put("headerMetrics", model.getMetrics());
        context.put("headerMetricsRaw", model.getRawMetrics());
        context.put("projectInfo", model);
        context.put("appPagePresent", Boolean.TRUE);
        context.put("testPagePresent", Boolean.TRUE);

        HtmlReportUtil.addFilteredPercentageToContext(context, model);

        insertCommonPropsForCurrent(context, "");

        final Map<String, PackageFragment> aggregatePkgs = newHashMap();
        for (PackageFragment root : model.getPackageRoots()) {
            gatherAggregatePackages(aggregatePkgs, root);
        }

        final List<PackageFragment> kids = newArrayList(aggregatePkgs.values());
        kids.sort(detailComparator);
        context.put("packageFragments", kids);
        context.put("tree", tree);
        HtmlReportUtil.addColumnsToContext(context, reportConfig.getColumns().getPkgColumns(), model, kids);
        HtmlReportUtil.mergeTemplateToFile(outfile, context, "agg-pkgs.vm");
    }

    private void renderBasePages() throws Exception {
        File outfile = new File(basePath, reportConfig.getMainFileName());
        final VelocityContext context = new VelocityContext();
        context.put("currentPageURL", reportConfig.getMainFileName());

        insertCommonPropsForCurrent(context, "");

        context.put("homepageURL", getHomepageValue());
        HtmlReportUtil.mergeTemplateToFile(outfile, context,
                reportConfig.getMainFileName());

        HtmlReportUtil.mergeTemplateToDir(basePath, "style.css", context);
        HtmlReportUtil.mergeTemplateToDir(basePath, "tree.css", context);

    }

    /**
     * If the config has a homepage set and it is defined in {@link #HTML_HOMEPAGE_VALUES}, the value will be returned.
     * Otherwise, the homepage as defined on the config will be returned. If no homepaeg is defined, then {@link
     * #HTML_HOMEPAGE_DEFAULT} is returned.
     *
     * @return the value to use for the homepage
     */
    private String getHomepageValue() {
        final String homepageKey = reportConfig.getHomepage() != null ? reportConfig.getHomepage() : HTML_HOMEPAGE_DEFAULT;
        return HTML_HOMEPAGE_VALUES.getOrDefault(homepageKey, homepageKey);
    }

    private void renderPackagesSummaryPage(String name, String templateName, VelocityContext context,
                                           ProjectInfo model, TreeInfo tree, boolean linkToClouds) throws Exception {
        final String filename = tree.getPathPrefix() + name;
        final File outfile = new File(basePath, filename);
        context.put("currentPageURL", filename);

        List<PackageInfo> packages = model.getAllPackages();

        packages.sort(detailComparator);

        insertCommonPropsForCurrent(context, "");
        context.put("linkToClouds", linkToClouds);
        context.put("projectInfo", model);
        context.put("headerMetrics", model.getMetrics());
        context.put("headerMetricsRaw", model.getRawMetrics());

        HtmlReportUtil.addFilteredPercentageToContext(context, model);

        context.put("packages", packages);
        context.put("tree", tree);
        context.put("appPagePresent", Boolean.TRUE);
        context.put("testPagePresent", Boolean.TRUE);
        HtmlReportUtil.addColumnsToContext(context, reportConfig.getColumns().getPkgColumns(), model, packages);
        HtmlReportUtil.mergeTemplateToFile(outfile, context, templateName);
    }

    private void renderPackagesSummaryPage(ProjectInfo model, TreeInfo tree, boolean linkToClouds) throws Exception {
        renderPackagesSummaryPage("pkg-summary.html",
                "pkgs-summary.vm",
                new VelocityContext(), model, tree, linkToClouds);
    }

    private void renderTestResultsPkgsSummaryPage() throws Exception {
        final File outfile = new File(basePath, "test-pkg-summary.html");
        final VelocityContext context = new VelocityContext();

        final ProjectInfo projectInfo = getFullModel().copy(
                hasMetrics ->
                        !(hasMetrics instanceof ClassInfo) || ((ClassInfo) hasMetrics).isTestClass()
        );
        List<PackageInfo> packages = projectInfo.getAllPackages();

        packages.sort(TEST_SORT_ORDER);

        context.put("currentPageURL", outfile.getName());
        insertCommonPropsForCurrent(context, "");
        insertCommonTestProps(context, packages, "package", null, projectInfo,
                "test-pkg-summary.html", "Project", "Packages");
        context.put("projectInfo", projectInfo);
        context.put("topLevel", Boolean.TRUE);
        HtmlReportUtil.mergeTemplateToFile(outfile, context,
                "test-pkg-summary.vm");
    }

    private void renderPkgClassesPage(
            String outfileName,
            String templateName,
            PackageInfo pkg,
            List<ClassInfo> classes,
            VelocityContext context,
            String currentTabName,
            boolean isTests) throws Exception {

        File outdir = pkg != null ? CloverUtils.createOutDir(pkg, basePath) : basePath;
        classes.sort(listComparator);

        final File outfile = new File(outdir, outfileName);
        context.put("currentPageURL", outfileName);

        String name = pkg != null ? pkg.getName() : "All Classes";
        insertCommonPropsForCurrent(context, name);
        context.put("packageInfo", pkg);
        context.put("classlist", classes);
        context.put("currentTabName", currentTabName);
        context.put("isTests", isTests);
        context.put("topLevel", pkg == null);
        context.put("title", "Classes");

        HtmlReportUtil.mergeTemplateToFile(outfile, context, templateName);
    }

    public static String renderHtmlBarTable(float pcCovered, int width, String customClass) throws Exception {
        return renderHtmlBarTable(pcCovered, width, customClass, "", "");
    }

    public static String renderHtmlBarTable(float pcCovered, int width,
                                            String customClass, String customBarPositive, String customBarNegative) throws Exception {

        final VelocityContext context = new VelocityContext();
        context.put("empty", pcCovered < 0);
        context.put("pccovered", pcCovered);
        context.put("sortValue", pcCovered);
        context.put("width", width);
        context.put("customClass", customClass);
        context.put("customBarPositive", customBarPositive);
        context.put("customBarNegative", customBarNegative);
        context.put("renderUtil", new HtmlRenderingSupportImpl());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        HtmlReportUtil.getVelocityEngine().mergeTemplate(
                HtmlReportUtil.getTemplatePath("bar-graph.vm"), "ASCII", context, out);

        out.close();
        return baos.toString();
    }

    private void renderPkgSummaryPage(PackageInfo pkg, TreeInfo tree,
                                      boolean appPagePresent, boolean testPagePresent, boolean linkToClouds,
                                      CloverExecutor<Object> queue) throws Exception {
        VelocityContext context = new VelocityContext();
        insertCommonPropsForCurrent(context, pkg.getName());

        queue.submit(
                new RenderPackageSummaryAction(context, basePath, reportConfig, pkg, detailComparator, tree, rederingHelper,
                        appPagePresent, testPagePresent, linkToClouds));
    }

    private void renderPkgCloudPages(PackageInfo pkg, TreeInfo tree,
                                     boolean appPagePresent, boolean testPagePresent,
                                     CloverExecutor<Object> queue) throws Exception {
        VelocityContext context = new VelocityContext();
        insertCommonPropsForCurrent(context, pkg.getName());

        queue.submit(
                new RenderPackageCoverageCloudAction(context, reportConfig, basePath, tree, pkg, appPagePresent, testPagePresent));
    }

    /**
     * Render tree map for a package and it's subpackages
     *
     * @see #renderProjectTreeMapPage(CloverExecutor)
     */
    private void renderPkgTreeMapPage(PackageInfo pkg, CloverExecutor<Object> queue) {
        // TODO not implemented
    }

    private void renderTestResultsPkgSummaryPages(@NotNull PackageInfo pkg,
                                                  @NotNull List<ClassInfo> classes) throws Exception {
        final File outdir = CloverUtils.createOutDir(pkg, basePath);

        final HasMetricsFilter filter = new TestMethodFilter();
        for (ClassInfo classInfo : classes) {
            ClassInfo testClassInfo = classInfo.copy(classInfo.getContainingFile(), filter);
            renderTestClassSummaryPage(testClassInfo);
        }
        classes.sort(TEST_SORT_ORDER);

        final File outfile = new File(outdir, "test-pkg-summary.html");
        final VelocityContext context = new VelocityContext();

        context.put("currentPageURL", "test-pkg-summary.html");
        context.put("projectInfo", getFullModel());
        context.put("appModelPresent",
                getConfiguredModel().getNamedPackage(pkg.getName()) != null);
        context.put("testModelPresent",
                getTestModel().getNamedPackage(pkg.getName()) != null);

        insertCommonPropsForCurrent(context, pkg.getName());
        insertCommonTestProps(context, classes, "class", pkg, pkg, "test-pkg-summary.html", "Package", "Test Classes");
        HtmlReportUtil.mergeTemplateToFile(outfile, context,
                "test-pkg-summary.vm");
    }

    private void renderTestClassSummaryPage(@NotNull ClassInfo classInfo) throws Exception {

        String outName = rederingHelper.getTestClassLink(false, classInfo);
        File outFile = CloverUtils.createOutFile(Objects.requireNonNull(classInfo.getContainingFile()), outName, basePath);

        final List<TestCaseInfo> tests = newArrayList(classInfo.getTestCases());

        tests.sort(TEST_CASE_COMPARATOR);

        final VelocityContext context = new VelocityContext();

        context.put("currentPageURL", outName);

        insertCommonPropsForCurrent(context, classInfo.getPackage().getName());
        context.put("projectInfo", getFullModel());
        String link = rederingHelper.getTestClassLink(false, classInfo);

        insertCommonTestProps(context, tests, "test", classInfo.getPackage(),
                classInfo, link, "Class", "Tests");

        HtmlReportUtil.mergeTemplateToFile(outFile, context,
                "test-class-summary.vm");
    }

    private void insertCommonTestProps(
            VelocityContext context,
            List<?> entities,
            String childEntityType,
            PackageInfo pkg,
            HasMetrics entity,
            String link,
            String title,
            String subtitle) {

        context.put("entities", entities);
        context.put("childEntityType", childEntityType);
        if (pkg != null) {
            context.put("packageName", pkg.getName());
            context.put("packageInfo", pkg);
        }
        context.put("entity", entity);
        context.put("entityLink", link);
        context.put("headerMetrics", entity.getMetrics());
        context.put("headerMetricsRaw", entity.getRawMetrics());

        HtmlReportUtil.addFilteredPercentageToContext(context, entity);

        context.put("topLevel", Boolean.FALSE);
        context.put("title", title);
        context.put("subtitle", subtitle);
        context.put("hasResults", getTestModel().hasTestResults());
        context.put("appPagePresent",
                pkg == null || getConfiguredModel().getNamedPackage(pkg.getName()) != null);
        context.put("testPagePresent", Boolean.TRUE);
    }

    static class TestMethodFilter implements HasMetricsFilter {
        @Override
        public boolean accept(HasMetrics hm) {
            return !(hm instanceof MethodInfo) || ((MethodInfo) hm).isTest();
        }
    }

    /**
     * a container class that describes what file hierarchy a particluar page is being rendered into
     */
    public static class TreeInfo {
        private final String pathPrefix;
        private final String name;

        public TreeInfo(String pathPrefix, String name) {
            this.pathPrefix = pathPrefix;
            this.name = name;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public String getName() {
            return name;
        }

        public String getLowercaseName() {
            return name.toLowerCase();
        }

        public String toString() {
            return getName();
        }
    }
}

