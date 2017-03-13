package com.atlassian.clover.reporters.console;

import com.atlassian.clover.CodeType;
import com.atlassian.clover.reporters.CommandLineArgProcessors;
import com.atlassian.clover.reporters.Current;

import java.util.Locale;

/**
 * Processing command line arguments specific to {@link ConsoleReporter}
 */
public class ConsoleReporterArgProcessors extends CommandLineArgProcessors {

    public static ArgProcessor CodeTypes = new ArgProcessor() {
        private final String CODE_TYPES_AS_STRING_LIST = ("\""
                + CodeType.APPLICATION + "\",\""
                + CodeType.TEST + "\",\""
                + CodeType.ALL + '"').toLowerCase(Locale.ENGLISH);

        private final String INVALID_CODE_TYPE_MESSAGE =
                "Invalid code type set. Valid values are: " + CODE_TYPES_AS_STRING_LIST;

        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-c") || args[i].equals("--codetype");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            try {
                ((ConsoleReporterConfig) cfg).setCodeType(CodeType.valueOf(args[i].toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException ex) {
                usage(INVALID_CODE_TYPE_MESSAGE);
            }
            return i;
        }

        @Override
        public String help() {
            return "    -c, --codetype <string>\tThe type of code to report on. Valid values are " + CODE_TYPES_AS_STRING_LIST + ".\n" +
                    "\t\t\t\tDefault value is \"" + CodeType.APPLICATION.name().toLowerCase(Locale.ENGLISH) + "\"\n\n";
        }
    };

    public static ArgProcessor Level = new ArgProcessor() {
        private final String REPORTING_LEVELS_AS_STRING_LIST = ("\""
                + ReportingLevel.SUMMARY + "\",\""
                + ReportingLevel.PACKAGE + "\",\""
                + ReportingLevel.CLASS + "\",\""
                + ReportingLevel.METHOD + "\",\""
                + ReportingLevel.STATEMENT
                + '"').toLowerCase(Locale.ENGLISH);

        private final String INVALID_LEVEL_MESSAGE =
                "Invalid reporting level set. Valid values are: " + REPORTING_LEVELS_AS_STRING_LIST;

        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-l") || args[i].equals("--level");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            i++;
            try {
                ((ConsoleReporterConfig) cfg).setLevel(ReportingLevel.valueOf(args[i].toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException ex) {
                usage(INVALID_LEVEL_MESSAGE);
            }
            return i;
        }

        @Override
        public String help() {
            return "    -l, --level <string>\tThe level of detail to report. Valid\n" +
                    "\t\t\t\tvalues are " + REPORTING_LEVELS_AS_STRING_LIST + ".\n" +
                    "\t\t\t\tDefault value is \"" + ReportingLevel.SUMMARY.name().toLowerCase(Locale.ENGLISH) + "\".\n\n";
        }
    };

    public static ArgProcessor UnitTests = new ArgProcessor() {
        @Override
        public boolean matches(String[] args, int i) {
            return args[i].equals("-u") || args[i].equals("--unittests");
        }

        @Override
        public int process(String[] args, int i, Current cfg) {
            ((ConsoleReporterConfig)cfg).setShowUnitTests(true);
            return i;
        }

        @Override
        public String help() {
            return "    -u,  --unittests\t\tShow unit tests results summary. Should be used with codetype=all or codetype=test.\n\n";
        }
    };
}
