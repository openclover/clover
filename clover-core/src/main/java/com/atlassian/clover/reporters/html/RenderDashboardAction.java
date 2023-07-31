package com.atlassian.clover.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.MetricsCollator;
import com.atlassian.clover.reporters.json.RenderTreeMapAction;
import com.atlassian.clover.reporters.util.CloverChartFactory;
import com.atlassian.clover.registry.metrics.HasMetricsSupport;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.openclover.util.Lists.newArrayList;

/**
 */
public class RenderDashboardAction implements Callable {

    private static final int DBRD_PROJECT_RISKS_COUNT = 20;
    private static final int DBRD_TOP_N_COUNT = 5;

    private final File mBasePath; // share - read only
    private final FullProjectInfo mConfiguredInfo; // shared - read only
    private final FullProjectInfo mProjectInfo; // shared - read only
    private final VelocityContext mContext; // not shared, read/write
    private final CloverChartFactory.ChartInfo mHistogram;
    private final CloverChartFactory.ChartInfo mScatter;
    private final Current reportConfig;

    public RenderDashboardAction(VelocityContext ctx, File basePath, FullProjectInfo configured, FullProjectInfo full,
                                 CloverChartFactory.ChartInfo histogram, CloverChartFactory.ChartInfo scatter, Current reportConfig) {
        mBasePath = basePath;
        mConfiguredInfo = configured;
        mProjectInfo = full;
        mContext = ctx;
        mHistogram = histogram;
        mScatter = scatter;
        this.reportConfig = reportConfig;
    }

    @Override
    public Object call() throws Exception {
        final File outfile = insertDashboardProperties();
        HtmlReportUtil.mergeTemplateToFile(outfile, mContext, "dashboard.vm");
        return null;
    }

    protected File insertDashboardProperties() throws Exception {
        // get data required for dashboard
        if (Boolean.TRUE != mContext.get("skipCoverageTreeMap")) {
            // render the package level treemap
            final RenderTreeMapAction tree = new RenderTreeMapAction(new VelocityContext(), reportConfig, mBasePath, mConfiguredInfo);
            tree.renderTreeMapJson("treemap-dash-json.js", "processTreeMapDashJson", false);
        }
        final List<? extends BaseClassInfo> classes = mConfiguredInfo.getClasses(new TestClassCoverageThresholdFilter());

        final ClassInfoStatsCalculator avgMethodCmpCalculator = new ClassInfoStatsCalculator.AvgMethodComplexityCalculator();
        final ClassInfoStatsCalculator pcCoveredEleCalculator = new ClassInfoStatsCalculator.PcCoveredElementsCalculator();
        final ClassInfoStatsCalculator eleCountCalculator = new ClassInfoStatsCalculator.ElementCountCalculator();

        final List<BaseClassInfo> amcOrder = newArrayList(classes);
        Collections.sort(amcOrder, new OrderedCalculatorComparator(
                new ClassInfoStatsCalculator[]{avgMethodCmpCalculator, pcCoveredEleCalculator, eleCountCalculator})
        );

        final List<BaseClassInfo> pceOrder = newArrayList(classes);
        Collections.sort(pceOrder, new OrderedCalculatorComparator(
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

        HtmlReportUtil.addFilteredPercentageToContext(mContext, mProjectInfo);

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
}
