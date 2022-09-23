package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CodeType;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.cfg.Percentage;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.metrics.BlockMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.metrics.PackageMetrics;
import com.atlassian.clover.registry.metrics.ProjectMetrics;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.Format;
import com.atlassian.clover.reporters.util.HistoricalSupport;
import com.atlassian.clover.reporters.util.MetricsDiffSummary;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import static clover.com.google.common.collect.Lists.newArrayList;

/**
 * Clover Ant Task to report pass/fail status of a test run against
 * given targets. Targets may be given overall and/or per package level.
 * The build may optionally fail if targets are not met
 *
 * @since Clover 1.1
 */
public class CloverPassTask extends AbstractCloverTask {

    private File historydir;

    private File[] historyFiles;

    private double threshold;

    private HistoricalSupport.HasMetricsWrapper model;

    private Map models;

    /** Overall percentage coverage required */
    private Percentage targetPC = null;

    /** method pc coverage required **/
    private Percentage methodTarget;

    /** statement pc coverage required **/
    private Percentage statementTarget;

    /** conditional pc coverage required **/
    private Percentage conditionalTarget;

    /**
     * Flag which indicates whether a build should fail if targets are not met.
     */
    private boolean haltOnFailure = false;

    private String failureProperty;

    final Current currentConfig = new Current();

    /**
     * A list of filesets holding all the test sources *
     */
    private List testResults = newArrayList();

    /**
     * The list of filesets of test sources;
     */
    private List testSources = newArrayList();

    /**
     * The type of code to log - application, test or all code.
     */
    private CodeType codeType = CodeType.APPLICATION;

    /**
     * Inner class to gather the package level pass/fail coverage targets.
     */
    public static class PackageRequirement {
        /** The name of the package */
        private String name;

        /** Regex to match package names */
        private String regex;

        /** The coverage target for this package */
        private Percentage target;

        /** method pc coverage required **/
        private Percentage methodTarget;

        /** statement pc coverage required **/
        private Percentage statementTarget;

        /** conditional pc coverage required **/
        private Percentage conditionalTarget;

        /**
         * Set the name of the package
         *
         * @param name the package name.
         */
        public void setName(String name) {
            this.name = name;
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }

        /**
         * The coverage target for this package as a percentage.
         *
         * @param target the percentage value for the required coverage.
         */
        public void setTarget(Percentage target) {
            this.target = target;
        }

        public Percentage getMethodTarget() {
            return methodTarget;
        }

        public void setMethodTarget(Percentage methodTarget) {
            this.methodTarget = methodTarget;
        }

        public Percentage getStatementTarget() {
            return statementTarget;
        }

        public void setStatementTarget(Percentage statementTarget) {
            this.statementTarget = statementTarget;
        }

        public Percentage getConditionalTarget() {
            return conditionalTarget;
        }

        public void setConditionalTarget(Percentage conditionalTarget) {
            this.conditionalTarget = conditionalTarget;
        }
    }

    public void setCodeType(String codeTypeAsString) {
        try {
            codeType = CodeType.valueOf(codeTypeAsString.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            //Handled later in execute()
            codeType = null;
        }
    }

    @Override
    public void init() throws BuildException {
        super.init();
        currentConfig.setFormat(Format.DEFAULT_XML);
    }

    /**
     * The list of packages for which a coverage target has been specified.
     */
    private List<PackageRequirement> packageRequirements = newArrayList();


    /**
     * Add a apcage spec to the list of packages to be checked for coverage.
     *
     * @param requirement the package rquirement including the apckage name
     *        and percentage coverage required.
     */
    public void addPackage(PackageRequirement requirement) {
        packageRequirements.add(requirement);
    }

    public void setHistorydir(File historydir) {
        this.historydir = historydir;
    }

    public File getHistorydir() {
        return historydir;
    }

    public void setThreshold(Percentage threshold) {
        this.threshold = threshold.getAsFloatFraction() * 100.0;
    }

    public double getThreshold() {
        return threshold;
    }

    /**
     * Set the overall system coverage target
     *
     * @param percentValue the percentage coverage target for the build
     */
    public void setTarget(Percentage percentValue) {
        this.targetPC = percentValue;
    }

    public Percentage getMethodTarget() {
        return methodTarget;
    }

    public void setMethodTarget(Percentage methodTarget) {
        this.methodTarget = methodTarget;
    }

    public Percentage getStatementTarget() {
        return statementTarget;
    }

    public void setStatementTarget(Percentage statementTarget) {
        this.statementTarget = statementTarget;
    }

    public Percentage getConditionalTarget() {
        return conditionalTarget;
    }

    public void setConditionalTarget(Percentage conditionalTarget) {
        this.conditionalTarget = conditionalTarget;
    }


    public void setFailureProperty(String failureProperty) {
        this.failureProperty = failureProperty;
    }

    public String getFailureProperty() {
        return failureProperty;
    }

    /**
     * set the filter which controls which items are included in the log report.
     *
     * @param filterSpec a comma separated set of values denoting the
     *        items to include in the report.
     */
    public void setFilter(String filterSpec) {
        currentConfig.getFormat().setFilter(filterSpec);
    }

    public void setSpan(Interval span) {
        currentConfig.setSpan(span);
    }

    public void addTestResults(FileSet fileset) {
        testResults.add(fileset);
    }

    public void setIncludeFailedTestCoverage(boolean include) {
        currentConfig.setIncludeFailedTestCoverage(include);
    }

    public void addTestSources(FileSet fileset) {
        testSources.add(fileset);
    }

    public List getTestSources() {
        return testSources;
    }

    private void initFileSets() {
        // gather test result files
        FilesetFileVisitor.Util.collectFiles(getProject(), testResults, new FilesetFileVisitor() {
            @Override
            public void visit(File file) {
                currentConfig.addTestResultFile(file);
            }
        });
        // gather test source files
        FilesetFileVisitor.Util.collectFiles(getProject(), testSources, new FilesetFileVisitor() {
            @Override
            public void visit(File file) {
                currentConfig.addTestSourceFile(file);
            }
        });
    }


    /**
     * Set the flag indicating whether the build will be failed for
     * failing to meet a target coverage. If this is false the coverage
     * target failures are logged
     *
     * @param haltOnFailure true if the build is to fail when targets are not met.
     */
    public void setHaltOnFailure(boolean haltOnFailure) {
        this.haltOnFailure = haltOnFailure;
    }

    /**
     * Do the work - checking the overall and per-package coverage against
     * the givenm targets.
     */
    @Override
    public void cloverExecute() {
        boolean passed = true;
        StringBuffer targetFailures = new StringBuffer("");

        initFileSets();
        String initString = resolveInitString();
        currentConfig.setInitString(initString);

        if (targetPC == null && methodTarget == null && statementTarget == null &&
                conditionalTarget == null && packageRequirements.size() == 0 && getHistorydir() == null) {
            throw new BuildException("You need to set either an overall target using one or more of the \"target\",\"methodTarget\"," +
                    "\"statementTarget\",\"conditionalTarget\" or \"historydir\" attribs, or specify atleast one nested <package> element.");
        }

        if (codeType == null) {
            throw new BuildException(
                "You need to set a valid code type. Valid values are: " +
                "\"" + CodeType.APPLICATION.name().toLowerCase(Locale.ENGLISH) + "\",\"" + CodeType.TEST.name().toLowerCase(Locale.ENGLISH) + "\",\"" + CodeType.ALL.name().toLowerCase() + "\"");
        }

        // validate any nested package elements
        for (PackageRequirement requirement : packageRequirements) {
            if ((requirement.target == null && requirement.methodTarget == null &&
                    requirement.statementTarget == null && requirement.conditionalTarget == null && getHistorydir() == null)
                    || (requirement.name == null && requirement.regex == null)) {
                throw new BuildException("The <package> element requires a "
                        + "\"name\" or \"regex\" attribute and one or more of the \"target\", \"methodTarget\"," +
                        "\"statementTarget\",\"conditionalTarget\" or \"historydir\" attributes.");
            }

            if (requirement.name != null && requirement.regex != null) {
                throw new BuildException("The <package> element requires either the \"name\" or \"regex\" attribute set, not both.");
            }
        }

        CloverDatabase db;
        try {
            db = currentConfig.getCoverageDatabase();
        } catch (CloverException e) {
            throw new BuildException("Unable to read Clover coverage database", e);
        }

        final FullProjectInfo projectInfo = db.getModel(codeType);

        ProjectMetrics metrics = (ProjectMetrics)projectInfo.getMetrics();
        Logger.getInstance().debug("coverage = " + metrics.getPcCoveredElements());

        passed = checkCoverageFor(metrics.getPcCoveredElements(), targetPC, targetFailures, "Total", "target") && passed;
        passed = checkCoverageFor(metrics.getPcCoveredMethods(), methodTarget, targetFailures, "Method", "target") && passed;
        passed = checkCoverageFor(metrics.getPcCoveredStatements(), statementTarget, targetFailures, "Statement", "target") && passed;
        passed = checkCoverageFor(metrics.getPcCoveredBranches(), conditionalTarget, targetFailures, "Conditional", "target") && passed;
        try {
            passed = checkHistoryDirCoverage(metrics.getPcCoveredElements(), targetFailures, "Total", null) && passed;
        }
        catch (IOException | CloverException e) {
            throw new BuildException(e);
        }

        for (final PackageRequirement requirement : packageRequirements) {
            List<PackageInfo> matchedPackages = newArrayList();
            if (requirement.name != null) {
                PackageInfo packageInfo = projectInfo.getNamedPackage(requirement.name);
                if (packageInfo == null) {
                    throw new BuildException("No coverage information for "
                            + "specified package: " + requirement.name);
                }
                matchedPackages.add(packageInfo);
            } else if (requirement.regex != null) {
                try {
                    matchedPackages.addAll(projectInfo.getPackages(new HasMetricsFilter() {
                        @Override
                        public boolean accept(HasMetrics node) {
                            return node.getName().matches(requirement.regex);
                        }
                    }));
                } catch (PatternSyntaxException e) {
                    throw new BuildException("Invalid package regular expression '" + requirement.regex + "': " + e.getMessage());
                }
            }
            for (PackageInfo packageInfo : matchedPackages) {
                String packageName = packageInfo.getName();
                PackageMetrics pm = (PackageMetrics) packageInfo.getMetrics();

                String errorPrefix = "Package " + packageName;
                passed = checkCoverageFor(pm.getPcCoveredElements(), requirement.target, targetFailures, errorPrefix + " total", "target") && passed;
                passed = checkCoverageFor(pm.getPcCoveredMethods(), requirement.methodTarget, targetFailures, errorPrefix + " method", "target") && passed;
                passed = checkCoverageFor(pm.getPcCoveredStatements(), requirement.statementTarget, targetFailures, errorPrefix + " statement", "target") && passed;
                passed = checkCoverageFor(pm.getPcCoveredBranches(), requirement.conditionalTarget, targetFailures, errorPrefix + " conditional", "target") && passed;
                try {
                    passed = checkHistoryDirCoverage(pm.getPcCoveredElements(), targetFailures, errorPrefix + " total", packageName) && passed;
                } catch (IOException | CloverException e) {
                    throw new BuildException(e);
                }
            }
        }

        if ((!passed) && targetFailures.lastIndexOf(StringUtils.LINE_SEP) == targetFailures.length() - StringUtils.LINE_SEP.length()) {
            targetFailures.delete(targetFailures.length() - StringUtils.LINE_SEP.length(), targetFailures.length());
        }

        if (!passed) {
            log("Coverage check FAILED");
            String failMessage = "The following coverage targets for " + getProject().getName() + " were not met: "
                    + StringUtils.LINE_SEP + targetFailures;
            log(failMessage, Project.MSG_ERR);
            if (failureProperty != null) {
                getProject().setProperty(failureProperty, targetFailures.toString());
            }

            if (haltOnFailure) {
                throw new BuildException("Build failed to meet Clover "
                        + "coverage targets: " + failMessage);
            }
        } else {
            log("Coverage check PASSED");
        }
    }

    private boolean checkCoverageFor(float coverage, Percentage targetCoverage, StringBuffer failures, String level, String target) {
        final DecimalFormat pcFormat = new DecimalFormat("###.#%");

        if (targetCoverage != null) {
            // Always return true if coverage value is undefined
            if (BlockMetrics.isUndefined(coverage)) {
                Logger.getInstance().debug("Recorded coverage = " + coverage + " means undefined value, so cannot compare against target coverage = "  + targetCoverage + ". Returning PASS (true).");
                return true;
            }

            // Otherwise compare with N fractional digits precision
            pcFormat.setMinimumFractionDigits(targetCoverage.getScale());            
            if (targetCoverage.compare(coverage) > 0) {
                failures.append(
                        String.format("%s coverage of %s did not meet %s of %s",
                                level, pcFormat.format(coverage), target,
                                pcFormat.format(targetCoverage.getAsFloatFraction())));
                failures.append(StringUtils.LINE_SEP);
                return false;
            }
            else {
                Logger.getInstance().debug("recorded coverage = " + pcFormat.format(coverage)
                        + "; target coverage = " + pcFormat.format(targetCoverage.getAsFloatFraction()));
                
            }
        }
        return true;
    }

    private boolean checkHistoryDirCoverage(float coverage, StringBuffer failures, String level, String pkg) throws IOException, CloverException {
        if (getHistorydir() != null) {
            historyFiles = getHistoryFiles();

            model = getLastModel();
            if (model != null) {
                HasMetrics then = HistoricalSupport.getFullMetrics(model, pkg);
                if (then != null) {
                    Percentage targetCoverage = new Percentage("" + (then.getMetrics().getPcCoveredElements() * 100.0 - threshold));
                    targetCoverage.setScale(2);

                    boolean passed = checkCoverageFor(coverage, targetCoverage, failures, level, "last history point target");
                    if (!passed) {
                        appendClassInfo(then, pkg, failures);
                    }
                    return passed;
                }
                else {
                    Logger.getInstance().debug("Package " + pkg + " is new, the last history point target is met.");
                }
            }
        }
        return true;
    }

    private void appendClassInfo(HasMetrics then, String pkg, StringBuffer failures) throws CloverException {
        HasMetrics now;
        if (pkg == null) {
            now = currentConfig.getCoverageDatabase().getModel(codeType);
        }
        else {
            now = currentConfig.getCoverageDatabase().getModel(codeType).getNamedPackage(pkg);
        }
        List<MetricsDiffSummary> added = HistoricalSupport.getClassesMetricsDifference(then, now, new Percentage("0"), false);
        for (MetricsDiffSummary diff : added) {
            final DecimalFormat diffFormat = new DecimalFormat("###.#");
            failures.append(String.format("  %s%% %s (Added)%s",
                    diffFormat.format(diff.getPc2()), diff.getName(), StringUtils.LINE_SEP));
        }
        List<MetricsDiffSummary> diffs = HistoricalSupport.getClassesMetricsDifference(then, now, new Percentage("0"), true);
        for (MetricsDiffSummary diff : diffs) {
            if (diff.getPcDiff() < 0) {
                final DecimalFormat diffFormat = new DecimalFormat("###.#");
                failures.append(String.format("  %s to %s%% %s%s",
                        diffFormat.format(diff.getPcDiff()), diffFormat.format(diff.getPc2()), diff.getName(), StringUtils.LINE_SEP));
            }
        }
    }

    private HistoricalSupport.HasMetricsWrapper getLastModel() throws IOException, CloverException {
        if (models == null) { //only get model once
            models = HistoricalSupport.getAllProjectMetrics(historyFiles);
            if (!models.isEmpty()) {
                Object[] modelArray = models.keySet().toArray();
                long currentVersion = currentConfig.getCoverageDatabase().getModel(codeType).getVersion();
                for (int i = modelArray.length - 1; i >= 0; i--) {
                    if ((Long) modelArray[i] < currentVersion) {
                        Logger.getInstance().debug("Comparing current version " + (new Date(currentVersion))
                                + " with history version " + (new Date((Long) modelArray[i])));
                        return (HistoricalSupport.HasMetricsWrapper) models.get(modelArray[i]);
                    }
                }
                Logger.getInstance().debug("History points exist but are newer than the database being checked.");
            }
            else {
                Logger.getInstance().debug("No history points exist.");
            }
        }
        return model;
    }

    private File[] getHistoryFiles() {
        if (historyFiles == null) { //only get history files once
            historyFiles = CloverReportTask.HistoricalEx.processHistoryIncludes(getProject(), null, getHistorydir());
        }
        return historyFiles;
    }

}
