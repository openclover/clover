package com.atlassian.clover.reporters.console;

import com.atlassian.clover.CloverDatabase;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.api.command.ArgProcessor;
import com.atlassian.clover.api.command.HelpBuilder;
import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullStatementInfo;
import com.atlassian.clover.registry.entities.LineInfo;
import com.atlassian.clover.registry.metrics.ClassMetrics;
import com.atlassian.clover.registry.metrics.ProjectMetrics;
import com.atlassian.clover.reporters.CloverReporter;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.Format;
import com.atlassian.clover.util.Color;
import org.openclover.runtime.util.Formatting;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.CodeTypes;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.InitString;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.Level;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.ShowInnerFunctions;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.ShowLambdaFunctions;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.SourcePath;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.Span;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.Title;
import static com.atlassian.clover.reporters.console.ConsoleReporterArgProcessors.UnitTests;
import static org.openclover.util.Lists.join;
import static org.openclover.util.Lists.newArrayList;

public class ConsoleReporter extends CloverReporter {

    private static final List<ArgProcessor<Current>> mandatoryArgProcessors = Collections.singletonList(
            InitString
    );

    @SuppressWarnings("unchecked")
    private static final List<ArgProcessor<Current>> optionalArgProcessors = newArrayList(
            CodeTypes,
            Level,
            SourcePath,
            Span,
            ShowInnerFunctions,
            ShowLambdaFunctions,
            Title,
            UnitTests
    );

    private static final List<ArgProcessor<Current>> allArgProcessors =
            join(mandatoryArgProcessors, optionalArgProcessors);

    /** use to log messages **/
    private static final Logger LOG = Logger.getInstance();

    /** Report settings */
    private final ConsoleReporterConfig cfg;


    public ConsoleReporter(ConsoleReporterConfig cfg) throws CloverException {
        super(cfg.getCoverageDatabase(), cfg);
        this.cfg = cfg;
    }

    public void report(PrintWriter out, CloverDatabase db) {
        out.print("Clover Coverage Report");
        if (cfg.getTitle() != null) {
            out.println(" - " + cfg.getTitle());
        } else {
            out.println();
        }
        out.println("Coverage Timestamp: " + new Date(db.getRecordingTimestamp()));
        out.println("Report for code   : " + cfg.getCodeType());

        List<? extends PackageInfo> packages = db.getModel(cfg.getCodeType()).getAllPackages();
        if (cfg.getLevel().isShowPackages()) {
            out.println();
            out.println("Package Summary - ");
        }

        for (PackageInfo aPackage : packages) {
            FullPackageInfo pkg = (FullPackageInfo) aPackage;
            if (cfg.getPackageSet() != null
                    && !cfg.getPackageSet().contains(pkg.getName())) {
                continue;
            }
            if (cfg.getLevel().isShowPackages()) {
                out.println(pkg.getName() + ": " + Formatting.getPercentStr(pkg.getMetrics().getPcCoveredElements()));
            }

            final List<? extends FileInfo> fileInfos = pkg.getFiles();
            for (FileInfo fileInfo : fileInfos) {
                final FullFileInfo fInfo = (FullFileInfo) fileInfo;

                if (cfg.getLevel().isShowClasses()) {
                    out.println("---------------------------------------");
                    out.println("File: " + fInfo.getPackagePath());

                    final List<? extends ClassInfo> classes = fInfo.getClasses();
                    for (ClassInfo cInfo : classes) {
                        final ClassMetrics metrics = (ClassMetrics) cInfo.getMetrics();
                        out.println("Package: " + cInfo.getPackage().getName());
                        printMetricsSummary(out, "Class: " + cInfo.getName(), metrics, cfg);
                    }
                }

                if (cfg.getLevel().isShowMethods() || cfg.getLevel().isShowStatements()) {
                    final LineInfo[] lines = fInfo.getLineInfo(cfg.isShowLambdaFunctions(), cfg.isShowInnerFunctions());
                    for (LineInfo info : lines) {
                        if (info != null) {
                            if (cfg.getLevel().isShowMethods()) {
                                reportMethodsForLine(out, fInfo, info);
                            }
                            if (cfg.getLevel().isShowStatements()) { // in this case means both statements and branches
                                reportStatementsForLine(out, fInfo, info);
                                reportBranchesForLine(out, fInfo, info);
                            }
                        }
                    }
                }
            }
        }

        out.println();
        out.println();
        ProjectMetrics overview = (ProjectMetrics)db.getModel(cfg.getCodeType()).getMetrics();
        printMetricsSummary(out, "Coverage Overview -", overview, cfg);
        out.flush();
    }

    protected void reportMethodsForLine(final PrintWriter out, final FullFileInfo fInfo, final LineInfo info) {
        final FullMethodInfo[] starts = info.getMethodStarts();
        if (starts.length > 0) {
            for (FullMethodInfo start : starts) {
                if (start.getHitCount() == 0) {
                    out.println(fInfo.getPhysicalFile().getAbsolutePath()
                            + ":" + start.getStartLine() + ":" + start.getStartColumn()
                            + ":" + start.getSimpleName()
                            + ": method not entered.");
                }
            }
        }
    }

    protected void reportStatementsForLine(final PrintWriter out, final FullFileInfo fInfo, final LineInfo info) {
        final FullStatementInfo[] stmts = info.getStatements();
        if (stmts.length > 0) {
            for (FullStatementInfo stmt : stmts) {
                if (stmt.getHitCount() == 0) {
                    out.println(fInfo.getPhysicalFile().getAbsolutePath() + ":" + stmt.getStartLine() + ":"
                            + stmt.getStartColumn() + ": statement not executed.");
                }
            }
        }
    }

    protected void reportBranchesForLine(final PrintWriter out, final FullFileInfo fInfo, final LineInfo info) {
        final BranchInfo[] branches = info.getBranches();
        if (branches.length > 0) {
            for (BranchInfo branch : branches) {
                final String prefix = fInfo.getPhysicalFile().getAbsolutePath() + ":"
                        + branch.getStartLine() + ":" + branch.getStartColumn()
                        + ": ";

                if (!branch.isInstrumented()) {
                    out.println(prefix + "was not instumented, due to assignment in expression.");
                } else if (branch.getTrueHitCount() == 0 && branch.getFalseHitCount() == 0) {
                    out.println(prefix + "branch not evaluated.");
                } else if (branch.getTrueHitCount() == 0) {
                    out.println(prefix + "true branch never taken.");
                } else if (branch.getFalseHitCount() == 0) {
                    out.println(prefix + "false branch never taken.");
                }
            }
        }
    }

    @Override
    protected void validate() throws CloverException {
        super.validate();
    }

    static void printMetricsSummary(PrintWriter out, String title, ClassMetrics metrics, ConsoleReporterConfig cfg) {
        out.println(title);
        out.println("Coverage:-");

        final String methodSummary = infoSummaryString(metrics.getNumCoveredMethods(), metrics.getNumMethods(), metrics.getPcCoveredMethods());
        final String stmtSummary = infoSummaryString(metrics.getNumCoveredStatements(), metrics.getNumStatements(), metrics.getPcCoveredStatements());
        final String branchSummary = infoSummaryString(metrics.getNumCoveredBranches(), metrics.getNumBranches(), metrics.getPcCoveredBranches());
        final String totalSummary = Color.make(Formatting.getPercentStr(metrics.getPcCoveredElements())).b().toString();

        out.print("      Methods: " + methodSummary); printPcBar(out, methodSummary, metrics.getPcCoveredMethods());
        out.print("   Statements: " + stmtSummary);   printPcBar(out, stmtSummary, metrics.getPcCoveredStatements());
        out.print("     Branches: " + branchSummary); printPcBar(out, branchSummary, metrics.getPcCoveredBranches());
        out.print("        Total: " + totalSummary);  printPcBar(out, totalSummary, metrics.getPcCoveredElements());

        out.println("Complexity:-");
        out.println("   Avg Method: " + Formatting.format3d(metrics.getAvgMethodComplexity()) );
        out.println("      Density: " + Formatting.format3d(metrics.getComplexityDensity()) );
        out.println("        Total: " + metrics.getComplexity());

        if ( (cfg != null) && (cfg.isShowUnitTests()) ) {
            out.println("Tests:-");
            out.println("    Available: " + metrics.getNumTests());
            out.println("     Executed: " + metrics.getNumTestsRun());
            out.println("       Passed: " + metrics.getNumTestPasses());
            out.println("       Failed: " + metrics.getNumTestFailures());
            out.println("       Errors: " + metrics.getNumTestErrors());
        }
    }

    private static void printPcBar(PrintWriter out, String summary, float amt) {

        // pad out to 32 spaces from the left.
        int padLen = 32 - summary.length();
        out.print(String.format("%" + (padLen) + "s", " "));

        final int len = 80;
        final int pc = (int)(amt * len);
        for (int i = 0; i <= len; i++) {
            if (i < pc) {
                out.print(Color.make(" ").bg().green());
            } else {
                out.print(Color.make(" ").bg().red());
            }
        }
        out.println();
    }

    private static String infoSummaryString(int covered, int total, float percent) {
        final StringBuilder buf = new StringBuilder();
        buf.append(covered).append("/").append(total);
        buf.append(" (").append(Color.make(Formatting.getPercentStr(percent)).b()).append(")");
        return buf.toString();
    }

    public static void main(String[] args) {
        loadLicense();
        System.exit(runReport(args));
    }

    public static int runReport(String[] args) {
        final ConsoleReporterConfig config = processArgs(args);
        if (canProceedWithReporting(config)) {
            try {
                return new ConsoleReporter(config).execute();
            } catch (Exception e) {
                Logger.getInstance().error("A problem was encountered while rendering the report: " + e.getMessage(), e);
            }
        }
        return 1;
    }

    public static ConsoleReporterConfig processArgs(String[] args) {
        final ConsoleReporterConfig cfg = new ConsoleReporterConfig();
        cfg.setFormat(Format.DEFAULT_TEXT);
        try {
            int i = 0;

            while (i < args.length) {
                for (ArgProcessor argProcessor : allArgProcessors) {
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

    private static void usage(String msg) {
        System.err.println();
        if (msg != null) {
            System.err.println("  *** ERROR: " + msg);
        }
        System.err.println();

        System.err.println(HelpBuilder.buildHelp(ConsoleReporter.class, mandatoryArgProcessors, optionalArgProcessors));

        System.err.println();
    }

    @Override
    protected int executeImpl() {
        report(new PrintWriter(System.out), database);
        return 0;
    }

}
