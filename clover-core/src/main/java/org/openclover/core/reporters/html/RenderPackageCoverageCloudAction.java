package org.openclover.core.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.util.CloverUtils;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class RenderPackageCoverageCloudAction extends RenderCoverageCloudAction {
    private List childAndDescendantClasses;
    private FullPackageInfo pkg;
    private final boolean appPagePresent;
    private final boolean testPagePresent;

    public RenderPackageCoverageCloudAction(VelocityContext context, CloverReportConfig reportConfig,
                                            File basePath, HtmlReporter.TreeInfo tree,
                                            FullPackageInfo pkg, boolean appPagePresent, boolean testPagePresent) {
        super(context, reportConfig, tree, sortedClassesFor(pkg), basePath);
        this.pkg = pkg;
        this.childAndDescendantClasses = sortedChildrenAndDescendantClassesFor(pkg);
        this.appPagePresent = appPagePresent;
        this.testPagePresent = testPagePresent;
    }

    private List sortedChildrenAndDescendantClassesFor(FullPackageInfo pkg) {
        return sortClasses(pkg.getClassesIncludingSubPackages(), HasMetricsSupport.CMP_LEX);
    }

    private static List sortClasses(List classes, Comparator comparator) {
        if (classes != null) {
            classes.sort(comparator);
        }
        return classes;
    }

    private static List sortedClassesFor(FullPackageInfo pkg) {
        return sortClasses(pkg.getClasses(), HasMetricsSupport.CMP_LEX);
    }

    @Override
    protected File createOutputDir() throws IOException {
        return CloverUtils.createOutDir(pkg, basePath);
    }

    @Override
    protected TabInfo createRisksTab() {
        return new TabInfo("Top Risks",
                "top-risks.html",
                "help_pkg_risks");
    }

    @Override
    protected void applySpecificProperties() {
        context.put("packageInfo", pkg);
        context.put("packageName", pkg.getName());
        context.put("headerMetrics", pkg.getMetrics());
        context.put("headerMetricsRaw", pkg.getRawMetrics());
        context.put("classlist", classes);
        context.put("appPagePresent", appPagePresent);
        context.put("testPagePresent", testPagePresent);
        context.put("topLevel", Boolean.FALSE);

        HtmlReportUtil.addFilteredPercentageToContext(context, pkg);
        
    }

    @Override
    protected void applyAxies(ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2) {
        applyAxis("shallow", axis1, axis2, classes);
        if (classes.size() < childAndDescendantClasses.size()) {
            context.put("showCloudDepthToggle", Boolean.TRUE);
            applyAxis("deep", axis1, axis2, childAndDescendantClasses);
        }
    }
}
