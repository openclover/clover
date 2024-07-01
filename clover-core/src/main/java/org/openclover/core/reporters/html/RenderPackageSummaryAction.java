package org.openclover.core.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.Column;
import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;
import org.openclover.core.util.CloverUtils;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

public class RenderPackageSummaryAction implements Callable<Object> {
    private final VelocityContext context;
    private final File basePath;
    private final PackageInfo pkg;
    private final boolean appPagePresent;
    private final boolean testPagePresent;
    private final boolean linkToClouds;
    private final List<ClassInfo> childClasses;
    private final HtmlReporter.TreeInfo tree;
    private final Comparator<HasMetrics> detailComparator;
    private final List<Column> columns;
    private final HtmlRenderingSupport helper;
    
    public RenderPackageSummaryAction(VelocityContext context, File basePath, CloverReportConfig cfg, PackageInfo pkg,
                                      Comparator<HasMetrics> detailComparator, HtmlReporter.TreeInfo tree, HtmlRenderingSupport helper,
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

    private void sortClasses(List<ClassInfo> classes, Comparator<HasMetrics> comparator) {
        if (classes != null) {
            classes.sort(comparator);
        }
    }
}
