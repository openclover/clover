package com.atlassian.clover.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.spi.reporters.html.source.HtmlRenderingSupport;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.util.CloverUtils;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

public class RenderPackageSummaryAction implements Callable {
    private final VelocityContext context;
    private final File basePath;
    private final FullPackageInfo pkg;
    private final boolean appPagePresent;
    private final boolean testPagePresent;
    private final boolean linkToClouds;
    private final List<? extends ClassInfo> childClasses;
    private final HtmlReporter.TreeInfo tree;
    private final Comparator detailComparator;
    private final List<Column> columns;
    private final HtmlRenderingSupport helper;
    
    public RenderPackageSummaryAction(VelocityContext context, File basePath, CloverReportConfig cfg, FullPackageInfo pkg,
                                      Comparator detailComparator, HtmlReporter.TreeInfo tree, HtmlRenderingSupport helper,
                                      boolean appPagePresent, boolean testPagePresent, boolean linkToClouds) {
        this.context = context;
        this.basePath = basePath;
        this.pkg = pkg;
        this.childClasses = pkg.getClasses();
        this.detailComparator = detailComparator;
        this.tree = tree;
        this.columns = cfg.getColumns().getClassColumnsCopy();
        this.helper = helper;
        this.appPagePresent = appPagePresent;
        this.testPagePresent = testPagePresent;
        this.linkToClouds = linkToClouds;
    }

    @Override
    public Object call() throws Exception {
        File outDir = CloverUtils.createOutDir(pkg, basePath);

        String summaryFilename = tree.getPathPrefix() + "pkg-summary.html";
        File outfile = new File(outDir, summaryFilename);

        sortClasses(childClasses, detailComparator);
        context.put("linkToClouds", linkToClouds);
        context.put("currentPageURL", summaryFilename);
        context.put("packageInfo", pkg);
        context.put("packageName", pkg.getName());
        context.put("headerMetrics", pkg.getMetrics());
        context.put("headerMetricsRaw", pkg.getRawMetrics());
        context.put("classlist", childClasses);
        context.put("tree", tree);
        context.put("appPagePresent", appPagePresent);
        context.put("testPagePresent", testPagePresent);
        context.put("topLevel", Boolean.FALSE);
        context.put("title", "Classes");

        HtmlReportUtil.addFilteredPercentageToContext(context, pkg);

        HtmlReportUtil.addColumnsToContext(context, columns, pkg, childClasses);
        HtmlReportUtil.mergeTemplateToFile(outfile, context, "pkg-summary.vm");
        return null;
    }

    private void sortClasses(List classes, Comparator comparator) {
        if (classes != null) {
            classes.sort(comparator);
        }
    }
}
