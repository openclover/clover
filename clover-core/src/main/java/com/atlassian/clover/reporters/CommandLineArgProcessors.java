package com.atlassian.clover.reporters;

import com.atlassian.clover.Logger;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.util.Path;

import java.io.File;

/**
 * A set of helper classes for parsing command line options for HTML, XML, PDF and JSON reporters.
 */
public class CommandLineArgProcessors {

    protected static void usage(String message) {
        System.err.println("  *** ERROR: " + message);
    }

    /**
     * For parsing command line arguments.
     */
    public static interface ArgProcessor {
        boolean matches(String[] args, int i);
        int process(String[] args, int i, Current cfg);
        String help();
    }

    public static ArgProcessor InitString = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-i") || args[i].equals("--initstring");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.setInitString(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    -i, --initstring <string>\tclover initstring\n\n";
        }
    };

    public static ArgProcessor OutputDirHtml = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-o") || args[i].equals("--outputdir");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.setOutFile(new File(args[i]));
            cfg.setMainFileName("index.html");
            return i;
        }

        @Override
        public String help() {
            return "    -o, --outputdir <dir>\toutput directory for generated HTML.\n\n";
        }
    };

    public static ArgProcessor OutputDirJson = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-o") || args[i].equals("--outputdir");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.setOutFile(new File(args[i]));
            return i;
        }

        @Override
        public String help() {
            return "    -o, --outputdir <dir>\toutput directory for generated JSON.\n\n";
        }
    };

    public static ArgProcessor OutputFile = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-o") || args[i].equals("--outfile");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.setOutFile(new File(args[i]));
            return i;
        }

        @Override
        public String help() {
            return "    -o,  --outfile <dir>\tthe file to write report to.\n\n";
        }
    };

    public static ArgProcessor AlwaysReport = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-a") || args[i].equals("--alwaysreport");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.setAlwaysReport(true);
            return i;
        }

        @Override
        public String help() {
            return "    -a,  --alwaysreport\t\tgenerate report even if there is no coverage to report on\n\n";
        }
    };

    public static ArgProcessor HideBars = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-b") || args[i].equals("--hidebars");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.getFormat().setShowBars(false);
            return i;
        }

        @Override
        public String help() {
            return "    -b, --hidebars\t\tdon't render coverage bars. default is false.\n\n";
        }
    };

    public static ArgProcessor BlackAndWhite = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-bw");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.getFormat().setBw(true);
            return i;
        }

        @Override
        public String help() {
            return "    -bw\t\t\t\tDon't colour syntax-hilight source - smaller html output.\n\n";
        }
    };

    public static ArgProcessor OrderBy = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-c") || args[i].equals("--orderby");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.getFormat().setOrderby(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    -c,  --orderby <compname>\tcomparator to use when listing packages and classes.\n" +
                    "\t\t\t\tvalid values are \"alpha\" - alphabetic, \"desc\" - coverage descending,\n"+
                    "\t\t\t\t\"asc\" - coverage ascending. default is \"asc\".\n\n";
        }
    };

    public static ArgProcessor DebugLogging = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-d") || args[i].equals("--debug");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            Logger.setDebug(true);
            return i;
        }

        @Override
        public String help() {
            return "    -d,  --debug \t\tswitch logging level to debug\n\n";
        }
    };

    public static ArgProcessor ShowEmpty = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-e") || args[i].equals("--showempty");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.getFormat().setShowEmpty(true);
            return i;
        }

        @Override
        public String help() {
            return "    -e, --showempty\t\tshow classes/packages even if they don't have any\n"+
                    "\t\t\t\tstatements, methods or conditionals. default is false\n\n";
        }
    };

    public static ArgProcessor Filter = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-f") || args[i].equals("--filter");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.getFormat().setFilter(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    -f,  --filter <string>\tcomma separated list of contexts to filter\n" +
                    "\t\t\t\twhen generating coverage reports. Most useful one is \"catch\".\n" +
                    "\t\t\t\tvalid values are \"static\",\"instance\",\"constructor\",\"method\",\"switch\",\n" +
                    "\t\t\t\t\"while\",\"do\",\"for\",\"if\",\"else\",\"try\",\"catch\",\"finally\",\"sync\"\n\n";
        }
    };

    public static ArgProcessor HideSources = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-h") || args[i].equals("--hidesrc");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.getFormat().setSrcLevel(false);
            return i;
        }

        @Override
        public String help() {
            return "    -h, --hidesrc\t\tdon't render source code. default is false.\n\n";
        }
    };

    public static ArgProcessor IncludeFailedTestCoverage = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-if") || args[i].equals("--includefailcoverage");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.setIncludeFailedTestCoverage(true);
            return i;
        }

        @Override
        public String help() {
            return "    -if, --includefailcoverage\tinclude coverage from failed tests. Default is false.\n\n";
        }
    };

    public static ArgProcessor LineInfo = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-l") || args[i].equals("--lineinfo");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.getFormat().setSrcLevel(true);
            return i;
        }

        @Override
        public String help() {
            return "    -l,  --lineinfo  \t\tinclude source-level coverage info\n\n";
        }
    };

    public static ArgProcessor NoCache = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-n") || args[i].equals("--nocache");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.getFormat().setNoCache(true);
            return i;
        }

        @Override
        public String help() {
            return "    -n, --nocache\t\tinsert no-cache browser directives in html output.\n\n";
        }
    };

    public static ArgProcessor PageSize = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-p") || args[i].equals("--pagesize");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.getFormat().setPageSize(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    -p,  --pagesize <size>\tPage size to use. Valid values are \"A4\",\"Letter\". Default is \"A4\".\n\n";
        }
    };

    public static ArgProcessor SourcePath = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-p") || args[i].equals("--sourcepath");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.setSourcepath(new Path(args[i]));
            return i;
        }

        @Override
        public String help() {
            return "    -p, --sourcepath\t\tThe sourcepath that Clover should search on for source files.\n\n";
        }
    };

    public static ArgProcessor Span = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-s") || args[i].equals("--span");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            try {
                cfg.setSpan(new Interval(args[i]));
            } catch (NumberFormatException e) {
                usage("Invalid interval format for span. Should be integer followed by unit (s, m, h, d, w).");
            }
            return i;
        }

        @Override
        public String help() {
            return "    -s, --span <interval>\tSpecifies how far back in time to\n" +
                    "\t\t\t\tinclude coverage recordings from since the last Clover build. In Interval\n" +
                    "\t\t\t\tformat. \"60s\" = 60 seconds, \"1h\" = 1 hour, \"1d\" = 1 day etc.\n\n";
        }
    };

    public static ArgProcessor ShowInnerFunctions = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-si") || args[i].equals("--showinner");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.setShowInnerFunctions(true);
            return i;
        }

        @Override
        public String help() {
            return "    -si,  --showinner\t\tshow inner functions in the report\n\n";
        }
    };

    public static ArgProcessor ShowLambdaFunctions = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-sl") || args[i].equals("--showlambda");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.setShowLambdaFunctions(true);
            return i;
        }

        @Override
        public String help() {
            return "    -sl,  --showlambda\t\tshow lambda functions in the report\n\n";
        }
    };

    public static ArgProcessor ShowUnique = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-su") || args[i].equals("--showunique");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            cfg.setShowUniqueCoverage(true);
            return i;
        }

        @Override
        public String help() {
            return "    -su,  --showunique\t\tcalculate and show unique per-test coverage in the report\n\n";
        }
    };

    public static ArgProcessor Title = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-t") || args[i].equals("--title");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            cfg.setTitle(args[i]);
            return i;
        }

        @Override
        public String help() {
            return "    -t, --title <string>\treport title.\n\n";
        }
    };

    public static ArgProcessor ThreadCount = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-tc") || args[i].equals("--threadcount");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            try {
                int threadCount = Integer.parseInt(args[i]);
                if (threadCount < 0) {
                    usage("Invalid thread count. Should be zero or a positive integer.");
                } else {
                    cfg.setNumThreads(threadCount);
                }
            } catch (NumberFormatException e) {
                usage("Invalid thread count. Should be an integer.");
            }
            return i;
        }

        @Override
        public String help() {
            return "    -tc, --threadcount <int>\tnumber of additional threads to be allocated to report generation. Default is 0.\n\n";
        }
    };

    public static ArgProcessor TabWidth = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-tw") || args[i].equals("--tabwidth");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            try {
                int tabwidth = Integer.parseInt(args[i]);
                if (tabwidth >= Format.MIN_TABWIDTH && tabwidth <= Format.MAX_TABWIDTH) {
                    cfg.getFormat().setTabWidth(tabwidth);
                } else {
                    usage("ignoring tab width value, out of range " + Format.MIN_TABWIDTH + ".." + Format.MAX_TABWIDTH);
                }
            } catch (NumberFormatException e) {
                usage("ignoring illegal tab width: " + e.getMessage());
            }
            return i;
        }

        @Override
        public String help() {
            return "    -tw, --tabwidth <int>\tnumber of spaces a tab should be rendered as. default is 4.\n\n";
        }
    };

    public static ArgProcessor VerboseLogging = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-v") || args[i].equals("--verbose");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            Logger.setVerbose(true);
            return i;
        }

        @Override
        public String help() {
            return "    -v,  --verbose \t\tswitch logging level to verbose\n\n";
        }
    };

}
