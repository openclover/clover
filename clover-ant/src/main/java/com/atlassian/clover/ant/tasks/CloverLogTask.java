package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.CodeType;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.recorder.PerTestCoverageStrategy;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.reporters.console.ConsoleReporter;
import com.atlassian.clover.reporters.console.ConsoleReporterConfig;
import com.atlassian.clover.reporters.filters.FileSetFilter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Sets.newHashSet;

/**
 * Clover Ant task to print the coverage to the Ant log or Ant property.
 *
 * <pre>
 *   &lt;clover-log
 *       initstring="/path/to/clover.db"
 *       level="summary|class|method|statement"
 *       filter="constructor,assert"
 *       span="30d"
 *       codeType="APPLICATION|TEST|ALL"
 *       outputProperty="my.clover.log.output"
 *       showUnitTests="true|false"
 *   &gt;
 *       &lt;fileset&gt;&lt;/fileset&gt;
 *       &lt;package name="com.foo"/&gt;
 *       &lt;sourcepath&gt;&lt;sourcepath/&gt;
 *       &lt;testSources&gt;&lt;testSources/&gt;
 *   &lt;/clover-log&gt;
 * </pre>
 *
 * See also <a href="https://openclover.org/doc/manual/latest/ant--clover-log.html">ant--clover-log.html</a>
 * @since Clover 1.1
 */
public class CloverLogTask extends AbstractCloverTask {

    /**
     * Enumerated Attribute to control the logging level
     */
    public static class Level extends EnumeratedAttribute {
        /**
         * Allowed log levels
         */
        private static final String[] VALUES
                = {"summary", "package", "class", "method", "statement"};

        /**
         * Get the allowed values of Level
         *
         * @return an array of the allowed values.
         */
        @Override
        public String[] getValues() {
            return VALUES;
        }
    }

    /**
     * Inner class to gather the packages to be logged.
     */
    public static class Package {
        /**
         * The name of the package
         */
        private String packageName;

        /**
         * Set the name of the package
         *
         * @param packageName the package name.
         */
        public void setName(String packageName) {
            this.packageName = packageName;
        }
    }

    /**
     * Standard clover filter spec to control what information is logged.
     */
    private String filterSpec;

    /**
     * The level of detail to report
     */
    private Level level;

    private List<Package> packages = newArrayList();

    private Interval span = Interval.DEFAULT_SPAN;

    private Path sourcepath;

    /**
     * The list of filesets of test sources;
     */
    private List<FileSet> testSources = newArrayList();

    /**
     * The type of code to log - application, test or all code.
     */
    private CodeType codeType = CodeType.APPLICATION;

    /**
     * Name of the Ant property in which console report will be stored.
     */
    private String outputProperty;

    /**
     * The list of filesets for which to Log.
     */
    private List<FileSet> filesets = newArrayList();

    /**
     * Whether to show unit test summary in report.
     */
    private boolean showUnitTests = false;




    public void addFileset(final FileSet fs) {
        filesets.add(fs);
    }

    public void addPackage(final Package packageInfo) {
        packages.add(packageInfo);
    }

    /**
     * Adds a path to sourcepath.
     */
    public void addSourcepath(final Path path) {
        if (sourcepath == null) {
            sourcepath = path;
        } else {
            sourcepath.append(path);
        }
    }

    public void addTestSources(final FileSet fileset) {
        testSources.add(fileset);
    }

    public void setCodeType(final String codeTypeAsString) {
        try {
            codeType = CodeType.valueOf(codeTypeAsString.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            //Handled later in validate()
            codeType = null;
        }
    }

    /**
     * set the filter which controls which items are included in the log report.
     *
     * @param filterSpec a comma separated set of values denoting the
     *                   items to include in the report.
     */
    public void setFilter(final String filterSpec) {
        this.filterSpec = filterSpec;
    }

    /**
     * Set the report detail level.
     * @param level the report level
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    public void setOutputProperty(final String outputProperty) {
        this.outputProperty = outputProperty;
    }

    /**
     * Sets whether to show unit test summary (total number of tests, number of tests run,
     * number of passed / failed / error)
     */
    public void setShowUnitTests(final boolean show) {
        showUnitTests = show;
    }

    public void setSpan(final Interval span) {
        this.span = span;
    }

    /**
     * Print the coverage information to the Ant log
     */
    @Override
    public void cloverExecute() {

        final String initString = resolveInitString();

        final ConsoleReporterConfig config = new ConsoleReporterConfig();
        config.setInitString(initString);
        if (level != null) {
            config.setLevel(level.getValue());
        } else {
            config.setLevel("summary");
        }

        if (!config.validate()) {
            throw new BuildException("configuration is not valid");
        }

        if (packages.size() != 0) {
            final Set<String> packageSet = newHashSet();
            for (final Package packageInfo : packages) {
                packageSet.add(packageInfo.packageName);
            }
            config.setPackageSet(packageSet);
        }

        config.setCodeType(codeType);
        config.setShowUnitTests(showUnitTests);

        try {
            final HasMetricsFilter filter = filesets.size() != 0 ? new FilesetFilter(getProject(), filesets) : HasMetricsFilter.ACCEPT_ALL;
            final CloverDatabase db = new CloverDatabase(initString, filter, getProject().getName(), filterSpec);

            // gather test source files
            final Set<File> testFiles = newHashSet();
            FilesetFileVisitor.Util.collectFiles(getProject(), testSources, file -> testFiles.add(file));

            db.loadCoverageData(
                    new CoverageDataSpec(
                            new FileSetFilter(newArrayList(testFiles)),
                            span.getValueInMillis(),
                            false, true, false, false,
                            PerTestCoverageStrategy.IN_MEMORY));

            if (sourcepath != null) {
                db.resolve(new AntPath(sourcepath));
            }

            final ConsoleReporter reporter = new ConsoleReporter(config);
            if (outputProperty != null) {
                final Writer out = new StringWriter();
                reporter.report(new PrintWriter(out), db);
                getProject().setProperty(outputProperty, out.toString());
            } else {
                final Writer out = new OutputStreamWriter(new LogOutputStream(this, Project.MSG_INFO));
                reporter.report(new PrintWriter(out), db);
                out.flush();
            }
        } catch (CloverException | IOException e) {
            throw new BuildException(e);
        }
    }
}
