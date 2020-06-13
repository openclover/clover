package com.atlassian.clover;

import clover.com.google.common.collect.Iterables;
import clover.com.google.common.collect.Lists;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.command.ArgProcessor;
import com.atlassian.clover.api.command.HelpBuilder;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cmdline.CloverInstrArgProcessors;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.instr.java.Instrumenter;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.SourceScanner;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;


/**
 * A commandline tool to instrument Java source code.
 */
public class CloverInstr {

    @SuppressWarnings("unchecked")
    private static final List<ArgProcessor<JavaInstrumentationConfig>> mandatoryArgProcessors = Lists.newArrayList(
            CloverInstrArgProcessors.SrcDir,
            CloverInstrArgProcessors.DestDir
    );

    @SuppressWarnings("unchecked")
    private static final List<ArgProcessor<JavaInstrumentationConfig>> optionalArgProcessors = Lists.newArrayList(
            CloverInstrArgProcessors.InitString,
            CloverInstrArgProcessors.DistributedCoverage,
            CloverInstrArgProcessors.Relative,
            CloverInstrArgProcessors.FlushPolicy,
            CloverInstrArgProcessors.FlushInterval,
            CloverInstrArgProcessors.Encoding,
            CloverInstrArgProcessors.InstrStrategy,
            CloverInstrArgProcessors.InstrLevel,
            CloverInstrArgProcessors.InstrLambda,
            CloverInstrArgProcessors.SourceLevelArg,
            CloverInstrArgProcessors.RecordTestResults,
            CloverInstrArgProcessors.DontQualifyJavaLang,
            CloverInstrArgProcessors.MethodContext,
            CloverInstrArgProcessors.MethodWithMetricsContext,
            CloverInstrArgProcessors.StatementContext,
            CloverInstrArgProcessors.TestSourceRoot,
            CloverInstrArgProcessors.TestSourceIncludes,
            CloverInstrArgProcessors.TestSourceExcludes,
            CloverInstrArgProcessors.TestSourceClass,
            CloverInstrArgProcessors.TestSourceMethod,
            CloverInstrArgProcessors.Verbose,
            CloverInstrArgProcessors.JavaSourceFile
    );

    private static final List<ArgProcessor<JavaInstrumentationConfig>> allArgProcessors = Lists.newArrayList(
            Iterables.concat(mandatoryArgProcessors, optionalArgProcessors));

    private JavaInstrumentationConfig cfg;

    private Logger log = Logger.getInstance();

    private CloverInstr() {

    }

    public static void main(String[] args) {
        System.exit(mainImpl(args));
    }

    /**
     * sames as "main", but returns 0 on success
     */
    public static int mainImpl(String[] args) {

        CloverStartup.loadLicense(Logger.getInstance());

        CloverInstr instr = new CloverInstr();

        if (!instr.processArgs(args)) {
            return 1;
        }

        return instr.execute();
    }

    public int execute() {
        log = Logger.getInstance();
        final Instrumenter instr = new Instrumenter(log, cfg);

        try {
            // scan the source dir if supplied
            if (cfg.getSourceDir() != null) {
                cfg.getSourceFiles().addAll(scanSrcDir(cfg.getSourceDir()));
            }

            // start instrumentation session in the database
            try {
                ContextStore.saveCustomContexts(cfg);
                instr.startInstrumentation();
            } catch (CloverException e) {
                log.error("Could not initialise Clover: " + e.getMessage());
                return 1;
            }

            // instrument files
            for (String srcFile : cfg.getSourceFiles()) {
                instr.instrument(new File(srcFile), cfg.getDestDir(), cfg.getEncoding());
            }

            // close the session
            instr.endInstrumentation();
        } catch (Exception e) {
            log.error("Instrumentation error", e);
            return 1;
        }
        return 0;
    }


    private List<String> scanSrcDir(final File dir) throws IOException {
        final List<String> files = Lists.newArrayList();
        SourceScanner scanner = new SourceScanner(dir, ".*\\.java");
        scanner.visit(new SourceScanner.Visitor() {

            @Override
            public void common(String path) {
                files.add(new File(dir, FileUtils.getNormalizedPath(path)).getAbsolutePath());
            }

            @Override
            public void onlyInSrc(String path) {
                files.add(new File(dir, FileUtils.getNormalizedPath(path)).getAbsolutePath());
            }

            @Override
            public void onlyInDest(String path) {
                // no-op
            }
        });

        return files;
    }

    private static void usage(PrintStream out, String msg) {
        out.println();
        if (msg != null) {
            out.println("  *** ERROR: " + msg);
        }

        out.println(HelpBuilder.buildHelp(CloverInstr.class, mandatoryArgProcessors, optionalArgProcessors));
    }

    private static void usage(String msg) {
        usage(System.err, msg);
    }

    private boolean processArgs(String[] args) {
        cfg = new JavaInstrumentationConfig();

        try {
            int i = 0;

            while (i < args.length) {
                boolean matched = false;
                for (ArgProcessor<JavaInstrumentationConfig> argProcessor : allArgProcessors) {
                    if (argProcessor.matches(args, i)) {
                        i = argProcessor.process(args, i, cfg);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    System.err.println("  *** WARN: Unknown option: " + args[i]);
                }
                i++;
            }

            if (!cfg.validate()) {
                usage(cfg.getValidationFailureReason());
                return false;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage("Missing a parameter.");
            return false;
        }

        return true;
    }


}
