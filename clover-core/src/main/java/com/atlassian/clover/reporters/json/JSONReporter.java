package com.atlassian.clover.reporters.json;


import clover.com.google.common.collect.Iterables;
import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.command.ArgProcessor;
import com.atlassian.clover.api.command.HelpBuilder;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.CloverReporter;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.Format;
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl;
import com.atlassian.clover.util.CloverExecutor;
import com.atlassian.clover.util.CloverExecutors;
import com.atlassian.clover.util.CloverUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.atlassian.clover.reporters.CommandLineArgProcessors.AlwaysReport;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.DebugLogging;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.IncludeFailedTestCoverage;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.InitString;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.OutputDirJson;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.ShowInnerFunctions;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.ShowLambdaFunctions;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.ThreadCount;
import static com.atlassian.clover.reporters.CommandLineArgProcessors.VerboseLogging;
import static org.openclover.util.Lists.newArrayList;

public class JSONReporter extends CloverReporter {

    @SuppressWarnings("unchecked")
    private static final List<ArgProcessor<Current>> mandatoryArgProcessors = newArrayList(
            InitString,
            OutputDirJson
    );

    @SuppressWarnings("unchecked")
    private static final List<ArgProcessor<Current>> optionalArgProcessors = newArrayList(
            AlwaysReport,
            DebugLogging,
            IncludeFailedTestCoverage,
            ShowInnerFunctions,
            ShowLambdaFunctions,
            ThreadCount,
            VerboseLogging
    );

    private static final List<ArgProcessor<Current>> allArgProcessors = newArrayList(
            Iterables.concat(mandatoryArgProcessors, optionalArgProcessors));

    private final HtmlRenderingSupportImpl renderingHelper;
    private final File basePath;

    public JSONReporter(CloverReportConfig config) throws CloverException {
        super(config);
        renderingHelper = new HtmlRenderingSupportImpl(config.getFormat(), true);
        basePath = config.getOutFile();
    }

    private Current getConfigAsCurrent() {
        return (Current)reportConfig;
    }

    @Override
    public int executeImpl() throws CloverException {
        final long currentStartTime = System.currentTimeMillis();

        final FullProjectInfo projectInfo = database.getAppOnlyModel();
        projectInfo.buildCaches();
        final List<? extends PackageInfo> allPackages = projectInfo.getAllPackages();

        try {
            CloverUtils.createDir(basePath);

            final CloverExecutor service = CloverExecutors.newCloverExecutor(getConfigAsCurrent().getNumThreads(), "Clover-JSON");
            Logger.getInstance().info("Generating JSON report to: " + getConfigAsCurrent().getOutFile().getAbsolutePath());

            RenderFileJSONAction.initThreadLocals();
            RenderMetricsJSONAction.initThreadLocals();

            service.submit(
                new RenderColophonJSONAction(new VelocityContext(), new File(getConfigAsCurrent().getOutFile(), "colophon.js"), getConfigAsCurrent()));

            service.submit(
                new RenderMetricsJSONAction(
                    new VelocityContext(),
                    projectInfo,
                    getConfigAsCurrent(),
                    new File(getConfigAsCurrent().getOutFile(), "project.js"),
                    renderingHelper));

            service.submit(
                new RenderCloudsJSONAction.ForProjects.OfTheirRisks(
                    projectInfo,
                    new VelocityContext(),
                    getConfigAsCurrent(),
                    getConfigAsCurrent().getOutFile()));
            service.submit(
                new RenderCloudsJSONAction.ForProjects.OfTheirQuickWins(
                    projectInfo,
                    new VelocityContext(),
                    getConfigAsCurrent(),
                    getConfigAsCurrent().getOutFile()));

            for (PackageInfo packageInfo : allPackages) {
                final FullPackageInfo pkg = (FullPackageInfo) packageInfo;

                Logger.getInstance().verbose("Processing package " + pkg.getName());
                final long start = System.currentTimeMillis();

                processPackage(pkg, service);

                final long total = System.currentTimeMillis() - start;
                if (Logger.isDebug()) {
                    Logger.getInstance().debug("Processed package: " + pkg.getName() +
                            " (" + pkg.getClasses().size() + " classes, " +
                            pkg.getMetrics().getNumTests() + " tests)" +
                            " in " + total + "ms");
                }
            }

            service.shutdown();
            final Interval timeOut = getConfigAsCurrent().getTimeOut();
            if (!service.awaitTermination(timeOut.getValueInMillis(), TimeUnit.MILLISECONDS)) {
                throw new CloverException("Timout of '" + timeOut + "' reached during report generation. " +
                    "Please increase this value and try again.");
            }
        } catch (Exception e) {
            throw new CloverException(e);
        } finally {
            RenderFileJSONAction.resetThreadLocals();
            RenderMetricsJSONAction.resetThreadLocals();
        }

        final long currentTotalTime = System.currentTimeMillis() - currentStartTime;
        final int pkgCount = allPackages.size();
        final long msPerPkg = pkgCount == 0 ? currentTotalTime : currentTotalTime / pkgCount;
        Logger.getInstance().info(
            "Done. Processed " + pkgCount + " packages in " + currentTotalTime + "ms (" + msPerPkg + "ms per package).");

        return 0;
    }

    private void processPackage(FullPackageInfo pkg, CloverExecutor service) throws Exception {
        final List<? extends FileInfo> files = pkg.getFiles();

        final FullProjectInfo projectInfo = database.getFullModel();
        projectInfo.buildCaches();

        final File basedir = CloverUtils.createOutDir(pkg, getConfigAsCurrent().getOutFile());
        final File outfile = new File(basedir, "package.js");

        service.submit(new RenderMetricsJSONAction(new VelocityContext(), pkg, getConfigAsCurrent(), outfile, renderingHelper));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirRisks(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, true));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirRisks(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, false));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirQuickWins(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, true));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirQuickWins(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, false));

        for (FileInfo file : files) {
            service.submit(new RenderFileJSONAction(
                    (FullFileInfo) file, renderingHelper, getConfigAsCurrent(), new VelocityContext(), database, projectInfo));
        }
    }

    public static void main(String[] args) {
        loadLicense();
        System.exit(runReport(args));
    }

    public static int runReport(String[] args) {
        final Current config = processArgs(args);
        if (canProceedWithReporting(config)) {
            try {
                return new JSONReporter(config).execute();
            } catch (Exception e) {
                Logger.getInstance().error("A problem was encountered while rendering the report: " + e.getMessage(), e);
            }
        }
        return 1;
    }

    private static void usage(String msg) {
        System.err.println();
        if (msg != null) {
            System.err.println("  *** ERROR: " + msg);
        }
        System.err.println();

        System.err.println(HelpBuilder.buildHelp(JSONReporter.class, mandatoryArgProcessors, optionalArgProcessors));

        System.err.println();
    }

    public static Current processArgs(String[] args) {
        final Current cfg = new Current();
        cfg.setFormat(Format.DEFAULT_JSON);
        try {
            int i = 0;

            while (i < args.length) {
                for (ArgProcessor<Current> argProcessor : allArgProcessors) {
                    if (argProcessor.matches(args, i)) {
                        i = argProcessor.process(args, i, cfg);
                    }
                }
                i++;
            }

            if (!cfg.validate()) {
                usage(cfg.getValidationFailureReason());
                return null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage("Missing a parameter.");
            return null;
        }
        return cfg;
    }
}
