package com.atlassian.clover.cmdline;

import clover.org.apache.commons.lang3.StringUtils;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.command.ArgProcessor;
import com.atlassian.clover.cfg.instr.MethodContextDef;
import com.atlassian.clover.cfg.instr.StatementContextDef;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import com.atlassian.clover.remote.DistributedConfig;
import com.atlassian.clover.spec.instr.test.TestClassSpec;
import com.atlassian.clover.spec.instr.test.TestMethodSpec;

import java.io.File;
import java.util.Locale;

public class CloverInstrArgProcessors {
    protected static void usage(String message) {
        System.err.println("  *** ERROR: " + message);
    }

    public static ArgProcessor<JavaInstrumentationConfig> SrcDir = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-s") || args[i].equals("--srcdir");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setSourceDir(new File(args[i]).getAbsoluteFile());
            return i;
        }

        @Override
        public String help() {
            return "    -s, --srcdir <dir>\t\t Directory containing source files to be instrumented. If omitted\n" +
                    "\t\t\t\t individual source files should be specified on the command line.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> DestDir = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-d") || args[i].equals("--destdir");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setDestDir(new File(args[i]).getAbsoluteFile());
            return i;
        }

        @Override
        public String help() {
            return "    -d, --destdir <dir>\t\t Directory where Clover should place the instrumented sources.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> InitString = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-i") || args[i].equals("--initstring");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setInitstring(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    -i, --initstring <file>\t Clover initstring. This is the path to the dbfile that"
                    + "\t\t\t\t will be used to construct/update to store coverage data.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> DistributedCoverage = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-dc") || args[i].equals("--distributedCoverage");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setDistributedConfig(new DistributedConfig(args[i]));
            return i;
        }

        @Override
        public String help() {
            return "    -dc, --distributedCoverage <string>\t Configuration for recording distributed pre-test coverage.\n"
                    + "\t\t\t\t Valid keys and default values are: ON | OFF | " + new DistributedConfig();
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> Relative = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-r") || args[i].equals("--relative");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            cfg.setRelative(true);
            return i;
        }

        @Override
        public String help() {
            return "    -r, --relative\t If specified, the initstring is treated as a relative path, "
                    + "\t\t\t\t rather than being converted to an absolute path."
                    + "\t\t\t\t This is useful for distributed testing environments.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> FlushPolicy = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-p") || args[i].equals("--flushpolicy");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            try {
                cfg.setFlushPolicyFromString(args[i]);
            } catch (CloverException e) {
                usage(e.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "   -p, --flushpolicy <policy>\t Set the flushpolicy Clover will use during coverage recording."
                    + "\t\t\t\t legal values are \"directed\",\"interval\",\"threaded\". Default is \"directed\". If"
                    + "\t\t\t\t either \"interval\" or \"threaded\" policies is used, the flushinterval must also be set"
                    + "\t\t\t\t using the -f option.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> FlushInterval = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-f") || args[i].equals("--flushinterval");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            try {
                final int flushInterval = Integer.parseInt(args[i]);
                if (flushInterval <= 0) {
                    usage("Invalid flush interval. Should be a positive integer.");
                } else {
                    cfg.setFlushInterval(flushInterval);
                }
            } catch (NumberFormatException e) {
                usage("Invalid flush interval. Should be a positive integer.");
            }
            return i;
        }

        @Override
        public String help() {
            return "   -f, --flushinterval <int>\t Set the interval between flushes (in millisecs). Only applies to\n"
                    + "\t\t\t\t \"interval\" or \"threaded\" flush policies.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> Encoding = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-e") || args[i].equals("--encoding");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setEncoding(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "   -e, --encoding <encoding>\t Set the File encoding to use when reading source files.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> InstrStrategy = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("--instrumentation");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setInstrStrategy(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "   --instrumentation <string>\t Set the instrumentation strategy. Valid values are \"field\" and\n"
                    + "\t\t\t\t \"class\". Default is \"class\".";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> InstrLevel = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("--instrlevel");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setInstrLevelStrategy(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    --instrlevel <string>\t Set the instrumentation level. Valid values are \"statement\" and"
                    + "\t\t\t\t \"method\". Default is \"statement\".";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> InstrLambda = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("--instrlambda");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            try {
                cfg.setInstrumentLambda(LambdaInstrumentation.valueOf(args[i].toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException ex) {
                usage("Invalid value: " + args[i] + ". " + ex.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "    --instrlambda <string>\t Set whether lambda functions shall be instrumented. Valid values are: "
                    + StringUtils.join(LambdaInstrumentation.values(), ", ").toLowerCase(Locale.ENGLISH) + "."
                    + "\t\t\t\t Default is " + LambdaInstrumentation.NONE + ".";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> SourceLevel = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("--source");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setSourceLevel(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    --source <level>\t\t Set the language level for Clover to use when parsing files.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> RecordTestResults = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("--recordTestResults");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            cfg.setRecordTestResults(Boolean.parseBoolean(args[i]));
            return i;
        }

        @Override
        public String help() {
            return "    --recordTestResults <true|false> \t If set to false, Clover will not record test results.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> DontQualifyJavaLang = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("--dontFullyQualifyJavaLang");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            cfg.setFullyQualifyJavaLang(false);
            return i;
        }

        @Override
        public String help() {
            return "    --dontFullyQualifyJavaLang \t\t If set, then java.lang will not be used in instrumented source.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> MethodContext = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-mc") || args[i].equals("--methodContext");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            try {
                // format: name=value, where value may have one or more '='
                cfg.addMethodContext(parseContextDef(args[i]));
            } catch (CloverException e) {
                usage("Could not parse custom method context definition: " + args[i] + ". " + e.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "    -mc --methodContext <name>=<regexp> \t\t Defines a single custom method context. May be supplied more than once. (\\ may be needed to prevent shell expansion)";
        }

        private MethodContextDef parseContextDef(String line) throws CloverException {
            final int index = line.indexOf('=');
            if (index <= 0 || (index+1 == line.length())) {
                throw new CloverException("Custom context definitions must be of the form: 'name=regexp'");
            }
            final MethodContextDef contextDef = new MethodContextDef();
            contextDef.setName(line.substring(0, index));
            contextDef.setRegexp(line.substring(index+1));
            return contextDef;
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> MethodWithMetricsContext = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-mmc") || args[i].equals("--methodWithMetricsContext");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            try {
                cfg.addMethodContext(parseContextDef(args[i]));
            } catch (CloverException e) {
                usage("Could not parse custom method context definition: " + args[i] + ". " + e.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "    -mmc --methodWithMetricsContext '<name>;<regexp>;<maxstmt>;<maxcplx>;<maxaggrstmt>;<maxaggrcplx>' \t\t Defines a single custom method context with code metrics.\n" +
                    "\t\t\tParameters shall be separated with a semicolon and passed as a single argument (quotes recommended).\n" +
                    "\t\t\tOptional parameters can be omitted by putting no value between semicolons.\n" +
                    "\t\t\t<name> context name\n" +
                    "\t\t\t<regexp> regular expression to match\n" +
                    "\t\t\t<maxcplx> maximum cyclomatic complexity, optional\n" +
                    "\t\t\t<maxstmt> maximum number of statements, optional\n" +
                    "\t\t\t<maxaggrcplx> maximum aggregated cyclomatic complexity, optional\n" +
                    "\t\t\t<maxaggrstmt> maximum number of aggregated statements, optional\n" +
                    "\t\t\tExample: -mce 'trivialGetter;public.*get.*\\(\\);;;1;1'\n" +
                    "\t\t\tThe argument may be supplied more than once.";
        }

        private MethodContextDef parseContextDef(String line) throws CloverException {
            final String parameters[] = line.split(";");
            if (parameters.length < 2 || parameters.length > 6) {
                throw new CloverException(String.format("Expected between 2 and 6 parameters, but found %d in '%s'",
                        parameters.length, line));
            }

            final MethodContextDef contextDef = new MethodContextDef();
            // mandatory
            contextDef.setName(parameters[0]);
            contextDef.setRegexp(parameters[1]);
            // optional
            if (parameters.length > 2 && !parameters[2].isEmpty()) {
                contextDef.setMaxComplexity(Integer.parseInt(parameters[2]));
            }
            if (parameters.length > 3 && !parameters[3].isEmpty()) {
                contextDef.setMaxStatements(Integer.parseInt(parameters[3]));
            }
            if (parameters.length > 4 && !parameters[4].isEmpty()) {
                contextDef.setMaxAggregatedComplexity(Integer.parseInt(parameters[4]));
            }
            if (parameters.length > 5 && !parameters[5].isEmpty()) {
                contextDef.setMaxAggregatedStatements(Integer.parseInt(parameters[5]));
            }
            return contextDef;
        }
    };


    public static ArgProcessor<JavaInstrumentationConfig> StatementContext = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-sc") || args[i].equals("--statementContext");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            try {
                // expected format: name=value, where value may have one or more '='
                cfg.addStatementContext(parseContextDef(args[i]));
            } catch (CloverException e) {
                usage("Could not parse custom statement context definition: " + args[i] + ". " + e.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "    -sc --statementContext <name>=<regexp>\t\t Defines a single custom statement context. May be supplied more than once.(\\ may be needed to prevent shell expansion)";
        }

        private StatementContextDef parseContextDef(String line) throws CloverException {
            final int index = line.indexOf('=');
            if (index <= 0 || (index+1 == line.length())) {
                throw new CloverException("Custom context definitions must be of the form: 'name=regexp'");
            }
            final StatementContextDef contextDef = new StatementContextDef();
            contextDef.setName(line.substring(0, index));
            contextDef.setRegexp(line.substring(index+1));
            return contextDef;
        }
    };

    private static FileClassMethodTestDetector reuseFileClassMethodTestDetector(JavaInstrumentationConfig cfg) {
        // we use isTestDetector to avoid instantiation of DefaultTestDetector via getTestDetector
        final FileClassMethodTestDetector td;
        if (cfg.isTestDetector()) {
            td = (FileClassMethodTestDetector) cfg.getTestDetector();
        } else {
            td = new FileClassMethodTestDetector();
            cfg.setTestDetector(td);
        }
        return td;
    }

    public static ArgProcessor<JavaInstrumentationConfig> TestSourceRoot = new ArgProcessor<JavaInstrumentationConfig>() {

        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-tsr") || args[i].equals("--testSourceRoot");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;

            final FileClassMethodTestDetector td = reuseFileClassMethodTestDetector(cfg);
            td.setRoot(args[i]);

            return i;
        }

        @Override
        public String help() {
            return "    -tsr --testSourceRoot <path>\t\t Root folder for test sources against which includes/excludes are checked.\n" +
                    "\t\t\tUse if any include or exclude pattern is defined. \n" +
                    "\t\t\tIf root is not declared, current working directory is assumed. Example:\n" +
                    "\t\t\t-tsr 'src/test/java'";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> TestSourceIncludes = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-tsi") || args[i].equals("--testSourceIncludes");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;

            final FileClassMethodTestDetector td = reuseFileClassMethodTestDetector(cfg);
            td.setIncludes(args[i]);

            return i;
        }

        @Override
        public String help() {
            return "    -tsi --testSourceIncludes <ant pattern>\t\t Which files shall be treated as test sources. Use if built-in test detector is insufficient.\n" +
                    "\t\t\tOne or more Ant patterns, comma separated. Example:\n" +
                    "\t\t\t-tsi '**/*Test.java,**/*IT.java'";
        }
    };


    public static ArgProcessor<JavaInstrumentationConfig> TestSourceExcludes = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-tse") || args[i].equals("--testSourceExcludes");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;

            final FileClassMethodTestDetector td = reuseFileClassMethodTestDetector(cfg);
            td.setExcludes(args[i]);

            return i;
        }

        @Override
        public String help() {
            return "    -tse --testSourceExcludes <ant pattern>\t\t Which files shall not be treated as test sources. Use if built-in test detector is insufficient.\n" +
                    "\t\t\tOne or more Ant patterns, comma separated. Example:\n" +
                    "\t\t\t-tse '**/TestBase*.java,**/*TestUtil.java'";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> TestSourceClass = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-tsc") || args[i].equals("--testSourceClass");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;

            try {
                final FileClassMethodTestDetector td = reuseFileClassMethodTestDetector(cfg);
                td.addTestClassSpec(parseTestClassSpec(args[i]));
            } catch (CloverException e) {
                usage("Could not parse test source classes definition: " + args[i] + ". " + e.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "    -tsc --testSourceClass '<name>;<package>;<annotation>;<superclass>;<javadoc tag>'\t\t Which classes shall be treated as test code. Use if built-in test detector is insufficient.\n" +
                    "\t\t\tYou can use this parameter multiple times. You can use regular expressions. You can use an empty value or omit semicolons at the end. Examples:\n" +
                    "\t\t\t-tsc '.*Test;com\\.acme\\..*'  - match by a package and a class name\n" +
                    "\t\t\t-tsc ';;@RunWith'  - match by a class annotation\n" +
                    "\t\t\t-tsc ';;;;@test'   - match by a javadoc tag";
        }

        private TestClassSpec parseTestClassSpec(String arg) throws CloverException {
            final String parameters[] = arg.split(";");
            if (parameters.length < 1 || parameters.length > 5) {
                throw new CloverException(String.format("Expected between 1 and 5 parameters, but found %d in '%s'",
                        parameters.length, arg));
            }

            final TestClassSpec classSpec = new TestClassSpec();
            if (!parameters[0].isEmpty()) {
                classSpec.setName(parameters[0]);
            }
            if (parameters.length > 1 && !parameters[1].isEmpty()) {
                classSpec.setPackage(parameters[1]);
            }
            if (parameters.length > 2 && !parameters[2].isEmpty()) {
                classSpec.setAnnotation(parameters[2]);
            }
            if (parameters.length > 3 && !parameters[3].isEmpty()) {
                classSpec.setSuper(parameters[3]);
            }
            if (parameters.length > 4 && !parameters[4].isEmpty()) {
                classSpec.setTag(parameters[4]);
            }
            return classSpec;
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> TestSourceMethod = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-tsm") || args[i].equals("--testSourceMethod");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            i++;
            try {
                final FileClassMethodTestDetector td = reuseFileClassMethodTestDetector(cfg);
                td.addTestMethodSpec(parseTestMethodSpec(args[i]));
            } catch (CloverException e) {
                usage("Could not parse test source methods definition: " + args[i] + ". " + e.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "    -tsm --testSourceMethod '<name>;<annotation>;<return type>;<javadoc tag>'\t\t Which methods shall be treated as test ones. Use if built-in test detector is insufficient.\n" +
                    "\t\t\tYou can use this parameter multiple times. You can use an empty value or omit semicolons at the end. Examples:\n" +
                    "\t\t\t-tsm 'test.*'  - match all methods prefixed with 'test'\n" +
                    "\t\t\t-tsm ';@Test;void'  - match all void methods annotated with '@Test'\n" +
                    "\t\t\t-tsm ';;;@test' - match all methods having a '@test' javadoc tag";
        }

        private TestMethodSpec parseTestMethodSpec(String arg) throws CloverException {
            final String parameters[] = arg.split(";");
            if (parameters.length < 1 || parameters.length > 4) {
                throw new CloverException(String.format("Expected between 1 and 4 parameters, but found %d in '%s'",
                        parameters.length, arg));
            }

            final TestMethodSpec methodSpec = new TestMethodSpec();
            if (!parameters[0].isEmpty()) {
                methodSpec.setName(parameters[0]);
            }
            if (parameters.length > 1 && !parameters[1].isEmpty()) {
                methodSpec.setAnnotation(parameters[1]);
            }
            if (parameters.length > 2 && !parameters[2].isEmpty()) {
                methodSpec.setReturnType(parameters[2]);
            }
            if (parameters.length > 3 && !parameters[3].isEmpty()) {
                methodSpec.setTag(parameters[3]);
            }
            return methodSpec;
        }
    };


    public static ArgProcessor<JavaInstrumentationConfig> Verbose = new ArgProcessor<JavaInstrumentationConfig>() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-v") || args[i].equals("--verbose");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            Logger.setVerbose(true);
            return i;
        }

        @Override
        public String help() {
            return "    -v, --verbose\t\t Enable verbose logging.";
        }
    };

    public static ArgProcessor<JavaInstrumentationConfig> JavaSourceFile = new ArgProcessor<JavaInstrumentationConfig>() {

        @Override
        public boolean matches(String[] args, int i) {
            return args[i].endsWith(".java");
        }

        @Override
        public int process(String[] args, int i, JavaInstrumentationConfig cfg) {
            cfg.addSourceFile(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "   *.java\\t\\t Specific source file to be instrumented.";
        }
    };

}
