package org.openclover.core.reporters.xml;

import org.openclover.core.CloverDatabase;
import org.openclover.core.api.command.ArgProcessor;
import org.openclover.core.api.command.HelpBuilder;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.model.XmlNames;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.LineInfo;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.registry.metrics.FileMetrics;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.registry.metrics.ProjectMetrics;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.CloverReporter;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.Format;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org_openclover_runtime.CloverVersionInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.openclover.core.reporters.CommandLineArgProcessors.AlwaysReport;
import static org.openclover.core.reporters.CommandLineArgProcessors.DebugLogging;
import static org.openclover.core.reporters.CommandLineArgProcessors.Filter;
import static org.openclover.core.reporters.CommandLineArgProcessors.IncludeFailedTestCoverage;
import static org.openclover.core.reporters.CommandLineArgProcessors.InitString;
import static org.openclover.core.reporters.CommandLineArgProcessors.LineInfo;
import static org.openclover.core.reporters.CommandLineArgProcessors.OutputFile;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowInnerFunctions;
import static org.openclover.core.reporters.CommandLineArgProcessors.ShowLambdaFunctions;
import static org.openclover.core.reporters.CommandLineArgProcessors.Span;
import static org.openclover.core.reporters.CommandLineArgProcessors.ThreadCount;
import static org.openclover.core.reporters.CommandLineArgProcessors.Title;
import static org.openclover.core.reporters.CommandLineArgProcessors.VerboseLogging;
import static org.openclover.core.util.Lists.join;
import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newHashMap;

public class XMLReporter extends CloverReporter {

    private static final List<ArgProcessor<Current>> mandatoryArgProcessors = newArrayList(
            InitString,
            OutputFile
    );

    private static final List<ArgProcessor<Current>> optionalArgProcessors = newArrayList(
            AlwaysReport,
            DebugLogging,
            Filter,
            IncludeFailedTestCoverage,
            LineInfo,
            Span,
            ShowInnerFunctions,
            ShowLambdaFunctions,
            Title,
            ThreadCount,
            VerboseLogging
    );

    private static final List<ArgProcessor<Current>> allArgProcessors =
            join(mandatoryArgProcessors, optionalArgProcessors);

    private final ContextSet contextSet;

    public XMLReporter(CloverReportConfig config) throws CloverException {
        this(config.getCoverageDatabase(), config);
    }

    public XMLReporter(CloverDatabase database, CloverReportConfig config) {
        super(database, config);
        this.contextSet = database.getContextSet(this.reportConfig.getFormat().getFilter());
    }

    @Override
    public int executeImpl() throws CloverException {
        try {
            if (!reportConfig.isAlwaysReport() && !database.hasCoverage()) {
                Logger.getInstance().warn("No coverage recordings found. No report will be generated.");
                return 1;
            } else {
                XMLWriter out = initWriter();

                Logger.getInstance().info("Writing report to '" + reportConfig.getOutFile() + "'");

                out.writeXMLDecl();

                Map<String, String> attribs = newHashMap();
                attribs.put(XmlNames.A_CLOVER, CloverVersionInfo.RELEASE_NUM);
                attribs.put(XmlNames.A_GENERATED, String.valueOf(System.currentTimeMillis()));
                out.writeElementStart(XmlNames.E_COVERAGE, attribs);

                writeProject(out, XmlNames.E_PROJECT, database.getAppOnlyModel());
                writeProject(out, XmlNames.E_TESTPROJECT, database.getTestOnlyModel());
                out.writeElementEnd(XmlNames.E_COVERAGE);
                out.close();

                return 0;
            }
        } catch (IOException e) {
            throw new CloverException("IO Exception: " + e.getMessage());
        }
    }

    private XMLWriter initWriter() throws IOException {
        OutputStream os;

        final File outFile = reportConfig.getOutFile();
        if (outFile.getParent() != null && !outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        if (reportConfig.isCompress()) {
            os = new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(outFile.toPath())));
        } else {
            os = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));
        }
        return new XMLWriter(os, "UTF-8");
    }

    /**
     * Get the package elements for this project.
     */
    private void writeProject(XMLWriter out, String enclosingTag, ProjectInfo proj) throws IOException {
        Map<String, String> attributes = newHashMap();
        if (reportConfig.getTitle() != null) {
            attributes.put(XmlNames.A_NAME, reportConfig.getTitle());
        }

        long ts = database.getRecordingTimestamp();

        // the user can override the coverage timestamp
        if (reportConfig.getEffectiveDate() != null) {
            ts = reportConfig.getEffectiveDate().getTime();
        }

        attributes.put(XmlNames.A_TIMESTAMP, String.valueOf(ts));
        out.writeElementStart(enclosingTag, attributes);
        writeMetrics(out, proj.getMetrics());

        List<PackageInfo> packages = proj.getAllPackages();
        if (packages.size() > 0) {
            boolean summaryReport = false;
            if (reportConfig instanceof Current) {
                summaryReport = ((Current)reportConfig).getSummary();
            }

            for (PackageInfo pkg : packages) {

                attributes = newHashMap();
                attributes.put(XmlNames.A_NAME, pkg.getName());

                out.writeElementStart(XmlNames.E_PACKAGE, attributes);

                writeMetrics(out, pkg.getMetrics());

                // if we are generating a full report, we want to add the data
                // below the package level.
                if (!summaryReport) {
                    writeFilesForPkg(out, pkg);
                }

                out.writeElementEnd(XmlNames.E_PACKAGE);
            }
        }
        out.writeElementEnd(enclosingTag);
    }

    private void writeMetrics(XMLWriter out, BlockMetrics metrics) throws IOException {

        Map<String, String> attribs = newHashMap();
        // output block metrics
        attribs.put(
                XmlNames.A_ELEMENTS, String.valueOf(metrics.getNumElements()));
        attribs.put(
                XmlNames.A_STATEMENTS, String.valueOf(metrics.getNumStatements()));
        attribs.put(
                XmlNames.A_CONDITIONALS, String.valueOf(metrics.getNumBranches()));
        attribs.put(
                XmlNames.A_COVEREDELEMENTS, String.valueOf(metrics.getNumCoveredElements()));
        attribs.put(
                XmlNames.A_COVEREDSTATEMENTS, String.valueOf(metrics.getNumCoveredStatements()));
        attribs.put(
                XmlNames.A_COVEREDCONDITIONALS, String.valueOf(metrics.getNumCoveredBranches()));
        attribs.put(
                XmlNames.A_COMPLEXITY, String.valueOf(metrics.getComplexity()));

        if (metrics instanceof ClassMetrics){
            ClassMetrics cm = (ClassMetrics)metrics;
            // output class metrics
            attribs.put(
                    XmlNames.A_METHODS, String.valueOf(cm.getNumMethods()));
            attribs.put(
                    XmlNames.A_COVEREDMETHODS, String.valueOf(cm.getNumCoveredMethods()));

            if (metrics.getNumTestsRun() > 0 && !(metrics instanceof FileMetrics)) {
                attribs.put(XmlNames.A_NUM_TEST_PASS, String.valueOf(metrics.getNumTestPasses()));
                attribs.put(XmlNames.A_NUM_TEST_FAIL, String.valueOf(metrics.getNumTestFailures()));
                attribs.put(XmlNames.A_NUM_TEST_RUNS, String.valueOf(metrics.getNumTestsRun()));
                attribs.put(XmlNames.A_TEST_DURATION, String.valueOf(metrics.getTestExecutionTime()));
            }
            
            if (metrics instanceof FileMetrics) {
                FileMetrics fm = (FileMetrics)metrics;
                // output file metrics
                attribs.put(XmlNames.A_CLASSES, String.valueOf(fm.getNumClasses()));
                attribs.put(XmlNames.A_LOC, String.valueOf(fm.getLineCount()));
                attribs.put(XmlNames.A_NCLOC, String.valueOf(fm.getNcLineCount()));

                if (metrics instanceof PackageMetrics) {
                    PackageMetrics pm = (PackageMetrics)metrics;
                    // output pkg metrics
                    attribs.put(XmlNames.A_FILES, String.valueOf(pm.getNumFiles()));

                    if (metrics instanceof ProjectMetrics) {
                        ProjectMetrics pjm = (ProjectMetrics)metrics;
                        // output proj metrics
                        attribs.put(XmlNames.A_PACKAGES, String.valueOf(pjm.getNumPackages()));
                    }
                }
            }
        }

        out.writeElement(XmlNames.E_METRICS, attribs);
    }

    private void writeFilesForPkg(XMLWriter out, PackageInfo pkg) throws IOException {
        //get the files contained within the package.
        final List<FileInfo> files = pkg.getFiles();

        for (FileInfo fileInfo : files) {
            final Map<String, String> attribs = newHashMap();

            attribs.put(XmlNames.A_NAME, fileInfo.getName());
            attribs.put(XmlNames.A_PATH, fileInfo.getPhysicalFile().getAbsolutePath());
            out.writeElementStart(XmlNames.E_FILE, attribs);
            writeMetrics(out, fileInfo.getMetrics());
            writeClassesForFile(out, fileInfo.getClasses());

            if (reportConfig.getFormat().getSrcLevel()) {
                writeLineInfo(out, fileInfo);
            }
            out.writeElementEnd(XmlNames.E_FILE);
        }
    }

    private void writeClassesForFile(XMLWriter out, List<ClassInfo> classes) throws IOException {
        for (ClassInfo info : classes) {
            final Map<String, String> attribs = newHashMap();
            attribs.put(XmlNames.A_NAME, info.getName());
            out.writeElementStart(XmlNames.E_CLASS, attribs);
            writeMetrics(out, info.getMetrics());
            out.writeElementEnd(XmlNames.E_CLASS);
        }
    }

    private void writeLineInfo(XMLWriter out, FileInfo finfo) throws IOException {
        final int linecount = finfo.getLineCount();
        final LineInfo[] linfo = finfo.getLineInfo( ((Current)reportConfig).isShowLambdaFunctions(),
                ((Current)reportConfig).isShowInnerFunctions() );

        for (int i = 1; i <= linecount; i++) {
            LineInfo info = linfo[i];
            if (linfo[i] != null) {

                final MethodInfo[] starts = info.getMethodStarts();
                if (starts.length > 0) {
                    for (MethodInfo start : starts) {
                        if (start.isFiltered(contextSet)) {
                            continue;
                        }

                        final Map<String, String> attribs = newHashMap();
                        attribs.put(XmlNames.A_LINENUM, String.valueOf(i));
                        attribs.put(XmlNames.A_LINETYPE, XmlNames.V_METHOD);
                        attribs.put(XmlNames.A_VISIBILITY, start.getVisibility());
                        attribs.put(XmlNames.A_COUNT, String.valueOf(start.getHitCount()));
                        attribs.put(XmlNames.A_COMPLEXITY, String.valueOf(start.getComplexity()));
                        attribs.put(XmlNames.A_METHOD_SIG, XMLWriter.escapeAttributeValue(start.getName()));
                        if (start.isTest()) {
                            final FullClassInfo clazz = (FullClassInfo) start.getContainingClass();
                            final TestCaseInfo tci = clazz.getTestCase(clazz.getQualifiedName() + "." + start.getSimpleName());
                            if (tci != null && tci.isHasResult()) {
                                attribs.put(XmlNames.A_TEST_SUCCESS, XMLWriter.escapeAttributeValue("" + tci.isSuccess()));
                                attribs.put(XmlNames.A_TEST_DURATION, XMLWriter.escapeAttributeValue(String.valueOf(tci.getDuration())));
                            }
                        }
                        out.writeElement(XmlNames.E_LINE, attribs);
                    }
                }

                final StatementInfo[] stmts = info.getStatements();
                if (stmts.length > 0) {
                    for (StatementInfo stmt : stmts) {
                        if (stmt.isFiltered(contextSet)) {
                            continue;
                        }

                        final Map<String, String> attribs = newHashMap();
                        attribs.put(XmlNames.A_LINENUM, String.valueOf(i));
                        attribs.put(XmlNames.A_LINETYPE, XmlNames.V_STMT);
                        attribs.put(XmlNames.A_COUNT, String.valueOf(stmt.getHitCount()));
                        out.writeElement(XmlNames.E_LINE, attribs);
                    }
                }

                final BranchInfo[] branches = info.getBranches();
                if (branches.length > 0) {
                    for (BranchInfo branch : branches) {
                        if (branch.isFiltered(contextSet)) {
                            continue;
                        }

                        Map<String, String> attribs = newHashMap();
                        attribs.put(XmlNames.A_LINENUM, String.valueOf(i));
                        attribs.put(XmlNames.A_LINETYPE, XmlNames.V_COND);
                        attribs.put(XmlNames.A_TRUECOUNT, String.valueOf(branch.getTrueHitCount()));
                        attribs.put(XmlNames.A_FALSECOUNT, String.valueOf(branch.getFalseHitCount()));
                        out.writeElement(XmlNames.E_LINE, attribs);
                    }
                }
            }
        }
    }


    private static CloverReportConfig processArgs(String[] args) {
        Current cfg = new Current();
        cfg.setFormat(Format.DEFAULT_XML);

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
                cfg = null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage("Missing a parameter.");
            cfg = null;
        }
        return cfg;
    }


    private static void usage(String msg) {
        System.err.println();
        if (msg != null) {
            System.err.println("  *** ERROR: " + msg);
        }
        System.err.println();

        System.err.println(HelpBuilder.buildHelp(XMLReporter.class, mandatoryArgProcessors, optionalArgProcessors));

        System.err.println();
    }

    public static void main(String[] args) {
        loadLicense();
        System.exit(runReport(args));
    }

    public static int runReport(String[] args) {
        CloverReportConfig config = processArgs(args);

        if (canProceedWithReporting(config)) {
            try {
                return new XMLReporter(config).execute();
            } catch (CloverException e) {
                Logger.getInstance().error("An error occurred while generating the report: " + e.getMessage(), e);
            }
        }
        return 1;
    }
}

