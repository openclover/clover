package org.openclover.core.reporters.json;

import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.api.command.ArgProcessor;
import org.openclover.core.api.command.HelpBuilder;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.cfg.Interval;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.CloverReporter;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.Format;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;
import org.openclover.core.util.CloverExecutor;
import org.openclover.core.util.CloverExecutors;
import org.openclover.core.util.CloverUtils;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.openclover.core.reporters.CommandLineArgProcessors.AlwaysReport;
import static org.openclover.core.reporters.CommandLineArgProcessors.DebugLogging;
import static org.openclover.core.reporters.CommandLineArgProcessors.IncludeFailedTestCoverage;
import static org.openclover.core.reporters.CommandLineArgProcessors.InitString;
import static org.openclover.core.reporters.CommandLineArgProcessors.OutputDirJson;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowInnerFunctions;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowLambdaFunctions;
import static org.openclover.core.reporters.CommandLineArgProcessors.ThreadCount;
import static org.openclover.core.reporters.CommandLineArgProcessors.VerboseLogging;
import static org.openclover.core.util.Lists.join;
import static org.openclover.core.util.Lists.newArrayList;

public class JSONReporter extends CloverReporter {

    private static final List<ArgProcessor<Current>> mandatoryArgProcessors = newArrayList(
            InitString,
            OutputDirJson
    );

    private static final List<ArgProcessor<Current>> optionalArgProcessors = newArrayList(
            AlwaysReport,
            DebugLogging,
            IncludeFailedTestCoverage,
            ShowInnerFunctions,
            ShowLambdaFunctions,
            ThreadCount,
            VerboseLogging
    );

    private static final List<ArgProcessor<Current>> allArgProcessors =
            join(mandatoryArgProcessors, optionalArgProcessors);

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

        final ProjectInfo projectInfo = database.getAppOnlyModel();
        projectInfo.buildCaches();
        final List<PackageInfo> allPackages = projectInfo.getAllPackages();

        try {
            CloverUtils.createDir(basePath);

            final CloverExecutor service = CloverExecutors.newCloverExecutor(getConfigAsCurrent().getNumThreads(),
                    "OpenClover-JSON");
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

            for (PackageInfo pkg : allPackages) {

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

    private void processPackage(PackageInfo pkg, CloverExecutor service) throws Exception {
        final List<FileInfo> files = pkg.getFiles();

        final ProjectInfo projectInfo = database.getFullModel();
        projectInfo.buildCaches();

        final File basedir = CloverUtils.createOutDir(pkg, getConfigAsCurrent().getOutFile());
        final File outfile = new File(basedir, "package.js");

        service.submit(new RenderMetricsJSONAction(new VelocityContext(), pkg, getConfigAsCurrent(), outfile, renderingHelper));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirRisks(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, true));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirRisks(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, false));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirQuickWins(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, true));
        service.submit(new RenderCloudsJSONAction.ForPackages.OfTheirQuickWins(new VelocityContext(), pkg, getConfigAsCurrent(), basedir, false));

        for (FileInfo file : files) {
            service.submit(
                    new RenderFileJSONAction(
                            file, renderingHelper, getConfigAsCurrent(), new VelocityContext(), database, projectInfo));
        }
    }

    public static void main(String[] args) {
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
