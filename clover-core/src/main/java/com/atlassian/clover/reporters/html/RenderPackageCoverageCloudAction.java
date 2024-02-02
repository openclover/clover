package com.atlassian.clover.reporters.html;

import clover.org.apache.velocity.VelocityContext;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.io.File;
import java.io.IOException;

import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.metrics.HasMetricsSupport;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.util.CloverUtils;

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
