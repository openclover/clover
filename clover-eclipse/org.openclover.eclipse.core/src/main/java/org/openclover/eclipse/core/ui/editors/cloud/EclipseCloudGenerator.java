package org.openclover.eclipse.core.ui.editors.cloud;

import org.apache.velocity.VelocityContext;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.reporters.CloudGenerator;
import org.openclover.core.reporters.html.ClassInfoStatsCalculator;
import org.openclover.core.reporters.html.HtmlReportUtil;
import org.openclover.core.reporters.html.TestClassFilter;
import org.openclover.core.util.CloverUtils;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;

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

        ProjectInfo appOnlyModel = database.getAppOnlyModel();

        List<ClassInfo> allClasses = appOnlyModel.getClasses(new TestClassFilter());
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

        List<PackageInfo> allPackages = appOnlyModel.getAllPackages();
        for (PackageInfo packageInfo : allPackages) {
            createDeepReport(
                    packageInfo.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(packageInfo, basePath),
                    PROJECT_RISKS_FILE_NAME,
                    CloverEclipsePluginMessages.PACKAGE_RISKS(),
                    packageInfo.getClassesIncludingSubPackages(),
                    new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                    new ClassInfoStatsCalculator.PcCoveredElementsCalculator());
            createShallowReport(
                    packageInfo.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(packageInfo, basePath),
                    PROJECT_RISKS_FILE_NAME,
                    CloverEclipsePluginMessages.PACKAGE_RISKS(),
                    packageInfo.getClasses(),
                    new ClassInfoStatsCalculator.AvgMethodComplexityCalculator(),
                    new ClassInfoStatsCalculator.PcCoveredElementsCalculator());

            createDeepReport(
                    packageInfo.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(packageInfo, basePath),
                    QUICK_WINS_FILE_NAME,
                    CloverEclipsePluginMessages.QUICK_WINS(),
                    packageInfo.getClassesIncludingSubPackages(),
                    new ClassInfoStatsCalculator.ElementCountCalculator(),
                    new ClassInfoStatsCalculator.CoveredElementsCalculator());
            createShallowReport(
                    packageInfo.getName().replace('.', '/') + "/",
                    CloverUtils.createOutDir(packageInfo, basePath),
                    QUICK_WINS_FILE_NAME,
                    CloverEclipsePluginMessages.QUICK_WINS(),
                    packageInfo.getClasses(),
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
            List<ClassInfo> classes,
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
            List<ClassInfo> classes,
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
