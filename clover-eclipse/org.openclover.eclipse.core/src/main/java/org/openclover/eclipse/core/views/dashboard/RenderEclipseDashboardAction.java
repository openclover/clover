package org.openclover.eclipse.core.views.dashboard;

import org.apache.velocity.VelocityContext;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.Column;
import org.openclover.core.reporters.Columns;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.MetricsCollator;
import org.openclover.core.reporters.html.ClassInfoStatsCalculator;
import org.openclover.core.reporters.html.OrderedCalculatorComparator;
import org.openclover.core.reporters.html.StatisticsClassInfoVisitor;
import org.openclover.core.reporters.html.TestClassCoverageThresholdFilter;
import org.openclover.core.reporters.html.TestClassFilter;
import org.openclover.core.reporters.util.CloverChartFactory;
import org.openclover.core.reporters.util.CloverChartFactory.ChartInfo;
import org.openclover.eclipse.core.velocity.VelocityUtil;
import org.openclover.runtime.util.Formatting;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.openclover.core.util.Lists.newArrayList;

/**
 * Copied from RenderDashboardAction and HtmlReportUtils
 */
public class RenderEclipseDashboardAction implements Callable<Object> {

    private static final int DBRD_PROJECT_RISKS_COUNT = 20;
    private static final int DBRD_TOP_N_COUNT = 5;

    private final File mBasePath; // share - read only
    private final ProjectInfo mConfiguredInfo; // shared - read only
    private final ProjectInfo mProjectInfo; // shared - read only
    private final VelocityContext mContext; // not shared, read/write
    private final CloverChartFactory.ChartInfo mHistogram;
    private final CloverChartFactory.ChartInfo mScatter;
    private final Current reportConfig;

    public RenderEclipseDashboardAction(VelocityContext ctx, File basePath,
                                        ProjectInfo configured, ProjectInfo full, ChartInfo histogram,
                                        ChartInfo scatter, Current currentConfig) {
        mBasePath = basePath;
        mConfiguredInfo = configured;
        mProjectInfo = full;
        mContext = ctx;
        mHistogram = histogram;
        mScatter = scatter;
        this.reportConfig = currentConfig;
    }
    
    public void applyCtxChanges() throws Exception {
        insertDashboardProperties();
    }

    @Override
    public Object call() throws Exception {
        final File outfile = insertDashboardProperties();
        VelocityUtil.mergeTemplateToFile(VelocityUtil.getVelocityEngine(), outfile, mContext, "dashboard.vm");
        return null;
    }

    private File insertDashboardProperties() throws Exception {
        // get data required for dashboard
        if (Boolean.TRUE != mContext.get("skipCoverageTreeMap")) {
            // render the package level treemap
            final RenderEclipseTreeMapAction tree = new RenderEclipseTreeMapAction(new VelocityContext(), mBasePath, mConfiguredInfo);
            tree.renderTreeMapJson("treemap-dash-json.js", "processTreeMapDashJson", false);
        }
        final List<ClassInfo> classes = mConfiguredInfo.getClasses(new TestClassCoverageThresholdFilter());

        final ClassInfoStatsCalculator avgMethodCmpCalculator = new ClassInfoStatsCalculator.AvgMethodComplexityCalculator();
        final ClassInfoStatsCalculator pcCoveredEleCalculator = new ClassInfoStatsCalculator.PcCoveredElementsCalculator();
        final ClassInfoStatsCalculator eleCountCalculator = new ClassInfoStatsCalculator.ElementCountCalculator();

        final List<ClassInfo> amcOrder = newArrayList(classes);
        amcOrder.sort(new OrderedCalculatorComparator(
                new ClassInfoStatsCalculator[]{avgMethodCmpCalculator, pcCoveredEleCalculator, eleCountCalculator}));

        final List<ClassInfo> pceOrder = newArrayList(classes);
        pceOrder.sort(new OrderedCalculatorComparator(
                new ClassInfoStatsCalculator[]{pcCoveredEleCalculator, avgMethodCmpCalculator, eleCountCalculator}));

        final StatisticsClassInfoVisitor amcVisitor = StatisticsClassInfoVisitor.visit(amcOrder, avgMethodCmpCalculator);
        final StatisticsClassInfoVisitor pceVisitor = StatisticsClassInfoVisitor.visit(pceOrder, pcCoveredEleCalculator);

        final MetricsCollator collator = new MetricsCollator();


        final Map<Integer, List<ClassInfo>> classMap = collator.rankProjectRisks(pceOrder, amcOrder);
        final List topRisks = collator.getTopRisks(classMap, DBRD_PROJECT_RISKS_COUNT);

        // Add coverage metric
        Column tpc = new Columns.TotalPercentageCovered();
        tpc.setFormat("longbar");
        mContext.put("tpcColumn", tpc);

        mContext.put("hasResults", mProjectInfo.hasTestResults());
        mContext.put("appPagePresent", Boolean.TRUE);
        mContext.put("testPagePresent", Boolean.TRUE);

        mContext.put("projectInfo", mProjectInfo);
        mContext.put("headerMetrics", mConfiguredInfo.getMetrics());
        mContext.put("headerMetricsRaw", mConfiguredInfo.getRawMetrics());

        addFilteredPercentageToContext(mContext, mProjectInfo);

        // get the 5 most complex packages
        List packages = mConfiguredInfo.getAllPackages();
        List complexPkgs = collator.getTopOfList(packages, DBRD_TOP_N_COUNT, HasMetricsSupport.CMP_COMPLEXITY);
        mContext.put("complexPkgs", complexPkgs);

        // get the 5 most complex classes
        final List allClasses = mConfiguredInfo.getClasses(new TestClassFilter());
        List complexClasses = collator.getTopOfList(allClasses, DBRD_TOP_N_COUNT, HasMetricsSupport.CMP_COMPLEXITY);
        mContext.put("complexClasses", complexClasses);

        mContext.put("leastTestedMethods", collator.getLeastTestedMethods(classes,
                reportConfig.isShowLambdaFunctions(), reportConfig.isShowInnerFunctions()));

        String filename = "dashboard.html";
        final File outfile = new File(mBasePath, filename);
        mContext.put("currentPageURL", filename);
        mContext.put("topRisks", topRisks);
        mContext.put("axisColor", pceVisitor);
        mContext.put("axisSize", amcVisitor);

        mContext.put("chartInfoHistogram", mHistogram);
        mContext.put("chartInfoScatter", mScatter);

        return outfile;
    }

    private static void addFilteredPercentageToContext(VelocityContext context, HasMetrics model) {
        float pcFiltered = getPercentageFiltered(model);
        if (pcFiltered > 0) {
            String percentFiltered = Formatting.getPercentStr(pcFiltered);
            context.put("percentFiltered", percentFiltered);
            context.put("showFilterToggle", hasFilteredMetrics(model));
        }
    }

    private static float getPercentageFiltered(HasMetrics model) {
        float rawElements = model.getRawMetrics().getNumElements();
        if (rawElements > 0) {
            final int numElements = model.getMetrics().getNumElements();
            return (1.0f - (numElements / rawElements));
        }
        return -1.0f;
    }

    private static boolean hasFilteredMetrics(HasMetrics model) {
        return model.getMetrics().getNumElements() != model.getRawMetrics().getNumElements();
    }
}
