package org.openclover.core.reporters.html;

import org.apache.velocity.VelocityContext;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.CloverReportConfig;

import java.io.File;
import java.util.List;

public class RenderProjectCoverageCloudsAction extends RenderCoverageCloudAction {
    protected ProjectInfo projectInfo;

    public RenderProjectCoverageCloudsAction(VelocityContext context, CloverReportConfig reportConfig,
                                             File basePath, HtmlReporter.TreeInfo tree, ProjectInfo projectInfo) {
        this(context, reportConfig, basePath, tree, projectInfo, sortedAppClassesFor(projectInfo));
    }

    public RenderProjectCoverageCloudsAction(VelocityContext context, CloverReportConfig reportConfig,
                                             File basePath, HtmlReporter.TreeInfo tree, ProjectInfo projectInfo,
                                             List<ClassInfo> classes) {
        super(context, reportConfig, tree, classes, basePath);
        this.projectInfo = projectInfo;
    }

    @Override
    protected File createOutputDir() {
        return basePath;
    }

    @Override
    protected TabInfo createRisksTab() {
        return new TabInfo("Top Risks",
                "top-risks.html",
                "help_pkg_risks");
    }

    private static List<ClassInfo> sortedAppClassesFor(ProjectInfo model) {
        final List<ClassInfo> classes = model.getClasses(new TestClassFilter());
        classes.sort(HasMetricsSupport.CMP_LEX);
        return classes;
    }

    @Override
    protected void applySpecificProperties() {
        context.put("projectInfo", projectInfo);
        context.put("headerMetrics", projectInfo.getMetrics());
        context.put("headerMetricsRaw", projectInfo.getRawMetrics());
        context.put("appPagePresent", Boolean.TRUE);
        context.put("testPagePresent", Boolean.TRUE);
        context.put("topLevel", Boolean.TRUE);

        HtmlReportUtil.addFilteredPercentageToContext(context, projectInfo);

    }

    @Override
    protected void applyAxies(ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2) {
        applyAxis("deep", axis1, axis2, classes);
    }
}
