package org.openclover.core.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.reporters.CloverReportConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import static org.openclover.core.util.Lists.newArrayList;

public abstract class RenderCoverageCloudAction implements Callable {
    protected final List classes;
    protected final File basePath;
    protected final VelocityContext context;
    protected final CloverReportConfig reportConfig;
    protected final HtmlReporter.TreeInfo tree;

    public RenderCoverageCloudAction(VelocityContext context, CloverReportConfig reportConfig,
                                     HtmlReporter.TreeInfo tree, List classes, File basePath) {
        this.context = context;
        this.reportConfig = reportConfig;
        this.tree = tree;
        this.classes = classes;
        this.basePath = basePath;
    }

    @Override
    public Object call() throws Exception {
        final List<TabInfo> allTabs = newArrayList();
        final TabInfo risksInfo = createRisksTab();
        final TabInfo quickWinsInfo = createQuickWinsTab();

        allTabs.add(risksInfo);
        allTabs.add(quickWinsInfo);

        renderQuickWins(createOutputDir(), allTabs, quickWinsInfo, tree);
        renderProjectRisks(createOutputDir(), allTabs, risksInfo, tree);

        return null;
    }

    protected abstract File createOutputDir() throws IOException;

    protected abstract TabInfo createRisksTab();

    protected TabInfo createQuickWinsTab() {
        return new TabInfo("Quick Wins", "quick-wins.html", "help_pkg_quick_wins");
    }

    protected void renderProjectRisks(File outDir, List<TabInfo> allTabs, TabInfo currentTab, HtmlReporter.TreeInfo tree) throws Exception {
        renderCloudPage(
            outDir,
            allTabs,
            new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
            new ClassInfoStatsCalculator.PcCoveredElementsCalculator(),
            currentTab,
            tree);
    }

    protected void renderQuickWins(File outDir, List<TabInfo> allTabs, TabInfo currentTab, HtmlReporter.TreeInfo tree) throws Exception {
        renderCloudPage(
            outDir,
            allTabs,
            new ClassInfoStatsCalculator.ElementCountCalculator(),
            new ClassInfoStatsCalculator.CoveredElementsCalculator(),
            currentTab,
            tree);
    }

    /**
     * @param outDir output directory
     * @param allTabs a map(&lt;String&gt;title, &lt;String&gt;href prefix)
     * @param axis1    the axis to use for the size
     * @param axis2    the axis to use for the color
     * @param currentTab the tab info for the current tab being rendered @throws Exception if an error occurs
     * @param tree the report tree used for navigation in report page headers
     */
    protected void renderCloudPage(File outDir, List<TabInfo> allTabs, ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2, TabInfo currentTab, HtmlReporter.TreeInfo tree) throws Exception {
        context.put("currentPageURL", currentTab.getFilename());
        context.put("tabs", allTabs);
        context.put("tree", tree);
        context.put("showCloudOwner", Boolean.TRUE);

        applySpecificProperties();

        applyAxies(axis1, axis2);

        context.put("title", currentTab.getTitle());

        HtmlReportUtil.mergeTemplateToFile(
                new File(outDir, currentTab.getFilename()),
                context, "cloud-page.vm");
    }

    protected void applyAxis(String prefix, ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2, List classes) {
        StatisticsClassInfoVisitor v2 = StatisticsClassInfoVisitor.visit(classes, axis2);
        StatisticsClassInfoVisitor v1 = StatisticsClassInfoVisitor.visit(v2.getClasses(), axis1);
        context.put(prefix + "axis", Boolean.TRUE);
        context.put(prefix + "axis1", v1);
        context.put(prefix + "axis2", v2);

    }

    protected abstract void applyAxies(ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2);

    protected abstract void applySpecificProperties();
}
