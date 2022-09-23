package com.atlassian.clover.eclipse.core.ui.editors.cloud;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.registry.entities.BasePackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.reporters.CloudGenerator;
import com.atlassian.clover.reporters.html.ClassInfoStatsCalculator;
import com.atlassian.clover.reporters.html.HtmlReportUtil;
import com.atlassian.clover.reporters.html.TestClassFilter;
import com.atlassian.clover.util.CloverUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class EclipseCloudGenerator {
    public static final String AGGREGATE_PREFIX = "aggregate-";
    public static final String PROJECT_RISKS_FILE_NAME = "proj-risks.html";
    public static final String QUICK_WINS_FILE_NAME = "quick-wins.html";
    private static final String TEMPLATE = "cloud-eclipse.vm";

    private final CloverDatabase database;
    private final File basePath;

    public EclipseCloudGenerator(CloverDatabase database, File basePath) {
        this.database = database;
        this.basePath = basePath;
    }

    public void execute() throws Exception {
        createResources();

        FullProjectInfo appOnlyModel = database.getAppOnlyModel();

        List allClasses = appOnlyModel.getClasses(new TestClassFilter());
        createDeepReport(
                "",
                basePath,
                PROJECT_RISKS_FILE_NAME,
                CloverEclipsePluginMessages.PROJECT_RISKS(),
                allClasses,
                new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                new ClassInfoStatsCalculator.PcCoveredElementsCalculator());

        createDeepReport(
                "",
                basePath,
                QUICK_WINS_FILE_NAME,
                CloverEclipsePluginMessages.QUICK_WINS(),
                allClasses,
                new ClassInfoStatsCalculator.ElementCountCalculator(),
                new ClassInfoStatsCalculator.CoveredElementsCalculator());

        List<? extends PackageInfo> allPackages = appOnlyModel.getAllPackages();
        for (PackageInfo packageInfo : allPackages) {
            BasePackageInfo pkg = (BasePackageInfo) packageInfo;
            createDeepReport(
                    pkg.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(pkg, basePath),
                    PROJECT_RISKS_FILE_NAME,
                    CloverEclipsePluginMessages.PACKAGE_RISKS(),
                    pkg.getClassesIncludingSubPackages(),
                    new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                    new ClassInfoStatsCalculator.PcCoveredElementsCalculator());
            createShallowReport(
                    pkg.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(pkg, basePath),
                    PROJECT_RISKS_FILE_NAME,
                    CloverEclipsePluginMessages.PACKAGE_RISKS(),
                    pkg.getClasses(),
                    new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                    new ClassInfoStatsCalculator.PcCoveredElementsCalculator());

            createDeepReport(
                    pkg.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(pkg, basePath),
                    QUICK_WINS_FILE_NAME,
                    CloverEclipsePluginMessages.QUICK_WINS(),
                    pkg.getClassesIncludingSubPackages(),
                    new ClassInfoStatsCalculator.ElementCountCalculator(),
                    new ClassInfoStatsCalculator.CoveredElementsCalculator());
            createShallowReport(
                    pkg.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(pkg, basePath),
                    QUICK_WINS_FILE_NAME,
                    CloverEclipsePluginMessages.QUICK_WINS(),
                    pkg.getClasses(),
                    new ClassInfoStatsCalculator.ElementCountCalculator(),
                    new ClassInfoStatsCalculator.CoveredElementsCalculator());
        }
    }

    private void createResources() throws Exception {
        VelocityContext context = new VelocityContext();
        HtmlReportUtil.mergeTemplateToDir(basePath, "style.css", context);
    }

    protected void createShallowReport(
            String offsetFromRoot,
            File dir,
            String fileName,
            String pageTitle,
            List classes,
            ClassInfoStatsCalculator calcAxis1,
            ClassInfoStatsCalculator calcAxis2) throws IOException {

        try (OutputStream outputStream = new FileOutputStream(new File(dir, fileName))) {
            final EclipseEditorLinkingHtmlRenderingSupport shallowAxisRender = new EclipseEditorLinkingHtmlRenderingSupport(offsetFromRoot + fileName);
            final CloudGenerator reportGenerator = createReportGenerator(pageTitle, outputStream, shallowAxisRender);
            reportGenerator.createReport(classes, calcAxis1, calcAxis2);
        }
    }

    protected void createDeepReport(
            String offsetFromRoot,
            File dir,
            String fileName,
            String pageTitle,
            List classes,
            ClassInfoStatsCalculator calcAxis1,
            ClassInfoStatsCalculator calcAxis2) throws IOException {

        try (OutputStream outputStream = new FileOutputStream(new File(dir, AGGREGATE_PREFIX + fileName))) {
            final EclipseEditorLinkingHtmlRenderingSupport deepAxisRender = new EclipseEditorLinkingHtmlRenderingSupport(offsetFromRoot + AGGREGATE_PREFIX + fileName);
            final CloudGenerator reportGenerator = createReportGenerator(pageTitle, outputStream, deepAxisRender);
            reportGenerator.createReport(classes, calcAxis1, calcAxis2);
        }
    }

    protected CloudGenerator createReportGenerator(
            String pageTitle,
            OutputStream outputStream,
            EclipseEditorLinkingHtmlRenderingSupport axisRenderer) throws IOException {

        final VelocityContext context = new VelocityContext();
        context.put("baseUrl", basePath.toURI().toURL().toExternalForm());
        context.put("showSrc", Boolean.TRUE);
        context.put("title", pageTitle);

        return new CloudGenerator(TEMPLATE, axisRenderer, outputStream, context);
    }

    public static String getRisksURIFor(File outputDir, boolean showAggregate) {
        return new File(outputDir, (showAggregate ? AGGREGATE_PREFIX : "") + PROJECT_RISKS_FILE_NAME).toURI().toString();
    }

    public static String getQuickWinsURIFor(File outputDir, boolean showAggregate) {
        return new File(outputDir, (showAggregate ? AGGREGATE_PREFIX : "") + QUICK_WINS_FILE_NAME).toURI().toString();
    }
}
