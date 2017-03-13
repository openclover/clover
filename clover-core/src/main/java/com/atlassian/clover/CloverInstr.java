package com.atlassian.clover;

import clover.org.apache.commons.lang3.StringUtils;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.cfg.instr.MethodContextDef;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import com.atlassian.clover.instr.java.Instrumenter;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.remote.DistributedConfig;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.SourceScanner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static clover.com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class CloverInstr {

    private JavaInstrumentationConfig cfg;

    private File inDir;
    private File outDir;
    private List<String> srcFiles = newArrayList();
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
            ContextStore.saveCustomContexts(cfg);            
            instr.startInstrumentation();
        }
        catch (CloverException e) {
            log.error("Could not initialise Clover: " + e.getMessage());
            return 1;
        }

        try {
            // scan the source dir if supplied
            if (inDir != null) {
               srcFiles.addAll(scanSrcDir(inDir));
            }
            for (String srcFile : srcFiles) {
                instr.instrument(new File(srcFile), outDir, cfg.getEncoding());
            }

            instr.endInstrumentation();
        } catch (Exception e) {
            log.error("Instrumentation error", e);
            return 1;
        }
        return 0;
    }



    private  List<String> scanSrcDir(final File dir) throws IOException {
        final List<String> files = newArrayList();
        SourceScanner scanner = new SourceScanner(dir, ".*\\.java");
        scanner.visit(new SourceScanner.Visitor() {

            @Override
            public void common(String path) throws IOException {
                files.add(new File(dir, FileUtils.getNormalizedPath(path)).getAbsolutePath());
            }

            @Override
            public void onlyInSrc(String path) throws IOException {
                files.add(new File(dir, FileUtils.getNormalizedPath(path)).getAbsolutePath());
            }

            @Override
            public void onlyInDest(String path) {
                // no-op
            }
        });

        return files;
    }

    private static void usage(String msg) {
        System.err.println();
        if (msg != null) {
            System.err.println("  *** ERROR: " + msg);
        }
        System.err.println();
        System.err.println("  USAGE: " + CloverInstr.class.getName() + " [OPTIONS] PARAMS [FILES...]");
        System.err.println();
        System.err.println("  PARAMS:");
        System.err.println("    -s, --srcdir <dir>\t\t Directory containing source files to be instrumented. If omitted");
        System.err.println("\t\t\t\t individual source files should be specified on the command line.");
        System.err.println();
        System.err.println("    -d, --destdir <dir>\t\t Directory where Clover should place the instrumented sources.");
        System.err.println();
        System.err.println();
        System.err.println("  OPTIONS:");
        System.err.println("    -i, --initstring <file>\t Clover initstring. This is the path to the dbfile that");
        System.err.println("\t\t\t\t will be used to construct/update to store coverage data.");
        System.err.println();
        System.err.println("    -dc, --distributedCoverage <string>\t Configuration for recording distributed pre-test coverage. ");
        System.err.println("\t\t\t\t Valid keys and default values are: ON | OFF | " + new DistributedConfig());
        System.err.println();
        System.err.println("    -r, --relative\t If specified, the initstring is treated as a relative path, ");
        System.err.println("\t\t\t\t rather than being converted to an absolute path.");                
        System.err.println("\t\t\t\t This is useful for distributed testing environments.");
        System.err.println();
        System.err.println("    -p, --flushpolicy <policy>\t Set the flushpolicy Clover will use during coverage recording.");
        System.err.println("\t\t\t\t legal values are \"directed\",\"interval\",\"threaded\". Default is \"directed\". If");
        System.err.println("\t\t\t\t either \"interval\" or \"threaded\" policies is used, the flushinterval must also be set");
        System.err.println("\t\t\t\t using the -f option.");
        System.err.println("    -f, --flushinterval <int>\t Set the interval between flushes (in millisecs). Only applies to");
        System.err.println("\t\t\t\t \"interval\" or \"threaded\" flush policies.");
        System.err.println("    -e, --encoding <encoding>\t Set the File encoding to use when reading source files.");
        System.err.println();
        System.err.println("    --instrumentation <string>\t Set the instrumentation strategy. Valid values are \"field\" and");
        System.err.println("\t\t\t\t \"class\". Default is \"class\".");
        System.err.println();
        System.err.println("    --instrlevel <string>\t Set the instrumentation level. Valid values are \"statement\" and");
        System.err.println("\t\t\t\t \"method\". Default is \"statement\".");
        System.err.println();
        System.err.println("    --instrlambda <string>\t Set whether lambda functions shall be instrumented. Valid values are: "
                + StringUtils.join(LambdaInstrumentation.values(), ", ").toLowerCase(Locale.ENGLISH) + ".");
        System.err.println("\t\t\t\t Default is " + LambdaInstrumentation.NONE + ".");
        System.err.println();
        System.err.println("    --source <level>\t\t Set the language level for Clover to use when parsing files.");
        System.err.println();
        System.err.println("    --recordTestResults <true|false> \t If set to false, Clover will not record test results.");
        System.err.println();
        System.err.println("    --dontFullyQualifyJavaLang \t\t If set, then java.lang will not be used in instrumented source.");
        System.err.println();
        System.err.println("    -mc --methodContext <name>=<regexp> \t\t Defines a single custom method context. May be supplied more than once. (\\ may be needed to prevent shell expansion)");
        System.err.println();
        System.err.println("    -sc --statementContext <name>=<regexp>\t\t Defines a single custom statement context. May be supplied more than once.(\\ may be needed to prevent shell expansion)");
        System.err.println();
        System.err.println("    -v, --verbose\t\t Enable verbose logging.");
    }

    private boolean processArgs(String[] args) {

        cfg = new JavaInstrumentationConfig();
         try {
            int i = 0;

             while (i < args.length) {
                 if (args[i].equals("-i") || args[i].equals("--initstring")) {
                     i++;
                     cfg.setInitstring(args[i]);
                 }
                 else if (args[i].equals("-r") || args[i].equals("--relative")) {
                     cfg.setRelative(true);
                 }
                 else if (args[i].equals("-s") || args[i].equals("--srcdir")) {
                     i++;
                     inDir = (new File(args[i])).getAbsoluteFile();
                 }
                 else if (args[i].equals("-d") || args[i].equals("--destdir")) {
                     i++;
                     outDir = (new File(args[i])).getAbsoluteFile();
                 }
                 else if (args[i].equals("-e") || args[i].equals("--encoding")) {
                     i++;
                     cfg.setEncoding(args[i]);
                 }
                 else if (args[i].equals("--recordTestResults")) {
                     i++;
                     cfg.setRecordTestResults(Boolean.valueOf(args[i]).booleanValue());
                 }
                 else if (args[i].equals("-dc") || args[i].equals("--distributedCoverage")) {
                     i++;
                    cfg.setDistributedConfig(new DistributedConfig(args[i]));
                 }
                 else if (args[i].equalsIgnoreCase("--dontFullyQualifyJavaLang")) {
                     cfg.setFullyQualifyJavaLang(false);
                 }
                 else if (args[i].equals("-p") || args[i].equals("--flushpolicy")) {
                     i++;
                     String policy = args[i];
                     try {
                         cfg.setFlushPolicyFromString(policy);
                     }
                     catch (CloverException e) {
                         usage(e.getMessage());
                         return false;
                     }
                 }
                 else if (args[i].equals("-f") || args[i].equals("--flushinterval")) {
                     i++;
                     try {
                         cfg.setFlushInterval(Integer.parseInt(args[i]));
                         if (cfg.getFlushInterval() <= 0) {
                             throw new NumberFormatException();
                         }
                     } catch (NumberFormatException e) {
                         usage("expecting a positive integer value for flush interval");
                         return false;
                     }
                 }
                 else if (args[i].equals("-u") || args[i].equals("--useclass")) {
                     i++;
                     log.warn("the useclass parameter has been deprecated and will be ignored.");
                 }
                 else if (args[i].equals("--source")) {
                     i++;
                     cfg.setSourceLevel(args[i]);
                 }
                 else if (args[i].equals("--instrumentation")) {
                     i++;
                     String instr = args[i];
                     cfg.setInstrStrategy(instr);
                 }
                 else if (args[i].equals("--instrlevel")) {
                     i++;
                     String instr = args[i];
                     cfg.setInstrLevelStrategy(instr);
                 } else if (args[i].equals("--instrlambda")) {
                     try {
                         i++;
                         cfg.setInstrumentLambda(LambdaInstrumentation.valueOf(args[i].toUpperCase(Locale.ENGLISH)));
                     } catch (IllegalArgumentException ex) {
                         usage("Invalid value: " + args[i] + ". " + ex.getMessage());
                         return false;
                     }
                 } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                     Logger.setVerbose(true);
                 }
                 else if (args[i].equals("-mc") || args[i].equals("--methodContext")) {
                     // expected in the format: name=value, where value may have one or more '='
                     i++;
                     try {
                         cfg.addMethodContext(parseContextDef(args[i]));
                     } catch (CloverException e) {
                         usage("Could not parse custom method context definition: " + args[i] + ". " + e.getMessage());
                         return false;
                     }
                 }
                 else if (args[i].equals("-sc") || args[i].equals("--statementContext")) {
                     // expected in the format: name=value, where value may have one or more '='
                     i++;
                     try {
                         cfg.addStatementContext(parseContextDef(args[i]));
                     } catch (CloverException e) {
                         usage("Could not parse custom statement context definition: " + args[i] + ". " + e.getMessage());
                         return false;
                     }
                 }
                 else if (args[i].endsWith(".java")) {
                     srcFiles.add(args[i]);
                 }
              i++;
            }

            if (cfg.getInitString() == null) {
                try {
                    cfg.createDefaultInitStringDir();
                } catch (CloverException e) {
                    usage("No --initstring value supplied, and default location could not be created: " + e.getMessage());
                    return false;
                }
            }

            if (inDir == null && srcFiles.size() == 0) {
                usage("No source files specified");
                return false;
            }
            else if (outDir == null) {
                usage("No Destination dir specified");
                return false;
            }
            else if ((cfg.getFlushPolicy() == InstrumentationConfig.INTERVAL_FLUSHING ||
                    cfg.getFlushPolicy() == InstrumentationConfig.THREADED_FLUSHING) && cfg.getFlushInterval() == 0) {
                usage("When using either \"interval\" or \"threaded\" flushpolicy, a flushinterval must be specified.");
                return false;
            }
            else {
                if (inDir != null) {
                    if (inDir.equals(outDir)) {
                        usage("Srcdir and destdir cannot be the same.");
                        return false;
                    }

                    // check to see that indir is not a parent of outdir
                    File outParent = outDir.getParentFile();
                    while (outParent != null) {
                        if (outParent.equals(inDir)) {
                            usage("Cannot specify a destdir that is a nested dir of the srcdir.");
                            return false;
                        }
                        outParent = outParent.getParentFile();
                    }

                    if (cfg.getFlushPolicy() == InstrumentationConfig.DIRECTED_FLUSHING && cfg.getFlushInterval() != 0) {
                        log.warn("ignoring flushinterval since flushpolicy is directed. To specify interval flushing, use -p interval.");
                    }
                }
                return true;
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            usage("Missing a parameter.");
        }
        return false;
    }

    private MethodContextDef parseContextDef(String line) throws CloverException {
        final int index = line.indexOf('=');
        if (index <= 0 || (index+1 == line.length())) {
            throw new CloverException("Custom context definitions must be of the form: 'name=regexp'");
        }
        final MethodContextDef contextDef = new MethodContextDef();
        contextDef.setName(line.substring(0, index));
        contextDef.setRegexp(line.substring(index+1, line.length()));
        return contextDef;
    }
}
