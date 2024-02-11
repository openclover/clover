package org.openclover.ant.tasks;

import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.cfg.StorageSize;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.Format;
import com.atlassian.clover.reporters.Historical;
import com.atlassian.clover.reporters.Type;
import com.atlassian.clover.reporters.CloverReporter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Maps.newHashMap;
import static org.openclover.util.Maps.newTreeMap;

/**
 * The Clover report task serves as a driver for the various different
 * clover reporting formats
 *
 * @since Clover 1.1
 */
public class CloverReportTask extends AbstractCloverTask {
    private boolean failOnError = true;
    private String projectName = null;

    /**
     * Adapter class which provides a version of the Current report
     * configuration suitable for Ant use - i.e. able to accept
     * filesets and able to resolve formats by reference
     */
    public static class CurrentEx extends Current implements AntExtension {
        private CloverFormatType format;
        private ColumnsEx columns;

        /**
         * The list of filesets for files to include in this report
         */
        private List<FileSet> filesets = newArrayList();

        /**
         * The list of filesets of test results;
         */
        private List<FileSet> testResults = newArrayList();

        /**
         * The list of filesets of test sources;
         */
        private List<FileSet> testSources = newArrayList();

        private Path sourcepath = null;

        private Project project = null;

        public void addFormat(CloverFormatType format) {
            this.format = format;
        }

        public void addColumns(ColumnsEx cols) {
            this.columns = cols;
        }

        @Override
        public void resolve(Project p) {
            project = p;
            if (columns != null) {
                super.setColumns(columns.resolveColumnsRef());
            }
            boolean needsNewFrame = true;
            String mainFileName = "index.html";
            if (format != null) {
                Format actualFormat = format.getActualFormat(p);
                setFormat(actualFormat);
                if (!isHTML(actualFormat)) {
                    needsNewFrame = false;
                    mainFileName = "";
                }

            }
            if (sourcepath != null) {
                setSourcepath(new AntPath(sourcepath));
            }
            setMainFileName(mainFileName);
            setNeedsNewFrame(needsNewFrame);
            initFileSets();
        }

        @Override
        public String getTypeName() {
            return "Current";
        }

        public void addFileSet(FileSet fileset) {
            filesets.add(fileset);
        }

        @Override
        public List<FileSet> getFilesets() {
            return filesets;
        }


        /**
         * Adds a path to sourcepath.
         *  @param path to add
         */
        public void addSourcepath(Path path) {
            if (sourcepath == null) {
                sourcepath = path;
            }
            else {
                sourcepath.append(path);
            }
        }

        public void addTestResults(FileSet fileset) {
            testResults.add(fileset);
        }

        public List<FileSet> getTestResults() {
            return testResults;
        }

        public void addTestSources(FileSet fileset) {
            testSources.add(fileset);
        }

        public List<FileSet> getTestSources() {
            return testSources;
        }

        private void initFileSets() {
            // gather global source files
            FilesetFileVisitor.Util.collectFiles(project, filesets, file -> addGlobalFileName(file.getAbsolutePath()));

            // gather test result files
            FilesetFileVisitor.Util.collectFiles(project, testResults, this::addTestResultFile);

            // gather test source files, ignoring if the dirs are missing
            FilesetFileVisitor.Util.collectFiles(project, testSources, true, this::addTestSourceFile);
        }
    }

    public static class ChartEx extends Historical.Chart {
        
        public void addConfiguredColumns(ColumnsEx cols) {
            super.addColumns(cols.resolveColumnsRef());
        }
    }

    public static class CoverageEx extends Historical.Coverage{

        public void addConfiguredColumns(ColumnsEx cols) {
            super.addColumns(cols.resolveColumnsRef());
        }
    }

    public static class MetricsEx extends Historical.Metrics {

        public void addConfiguredColumns(ColumnsEx cols) {
            super.addColumns(cols.resolveColumnsRef());
        }
    }
    

    /**
     * Adapter class which provides a version of the Historical report
     * configuration suitable for Ant use - i.e. able to accept
     * filesets and able to resolve formats by reference
     */
    public static class HistoricalEx extends Historical implements AntExtension {
        private CloverFormatType format;

        /**
         * The list of filesets for files to include in this report
         */
        private List<FileSet> filesets = newArrayList();

        private String historyIncludes;

        public void addFormat(CloverFormatType format) {
            this.format = format;
        }


        public void addChart(ChartEx ex) {
            super.addChart(ex);
        }

        public void addCoverage(CoverageEx ex) {
            super.addCoverage(ex);
        }

        public void addMetrics(MetricsEx ex) {
            super.addMetrics(ex);
        }

        @Override
        public void resolve(Project p) {
            String mainFileName = "historical.html";
            if (format != null) {
                Format actualFormat = format.getActualFormat(p);
                setFormat(actualFormat);
                if (!actualFormat.in(Type.HTML)) {
                    mainFileName = "";
                }
            }
            setMainFileName(mainFileName);
            setNeedsNewFrame(false);
            historyFiles = processHistoryIncludes(p);
        }

        public void addFileSet(FileSet fileset) {
            filesets.add(fileset);
        }

        @Override
        public List<FileSet> getFilesets() {
            return filesets;
        }

        @Override
        public String getTypeName() {
            return "Historical";
        }

        public void setHistoryIncludes(String includesSpec) {
            historyIncludes = includesSpec;
        }

        private File[] processHistoryIncludes(Project project) {
            return processHistoryIncludes(project, historyIncludes, getHistoryDir());
        }

        public static File[] processHistoryIncludes(Project project, String includes, File historyDir) {
            if (includes == null) {
                includes = CloverNames.HISTPOINT_PREFIX + "*" + CloverNames.HISTPOINT_SUFFIX;
            }
            DirectoryScanner dirScanner = new DirectoryScanner();
            PatternSet patterns = new PatternSet();
            patterns.setIncludes(includes);
            Logger.getInstance().debug("Using historyIncludes of: '" + includes + "'");
            dirScanner.setBasedir(historyDir);
            dirScanner.setIncludes(patterns.getIncludePatterns(project));
            dirScanner.scan();
            String[] filePaths = dirScanner.getIncludedFiles();
            File[] files = new File[filePaths.length];
            for (int i = 0; i < filePaths.length; i++) {
                String filePath = filePaths[i];
                files[i] = new File(dirScanner.getBasedir(), filePath);
                Logger.getInstance().debug("Including history point file: '" + files[i].getAbsolutePath() + "'");
            }
            return files;
        }
        
    }

    /**
     * The list of reports to run in this report batch
     */
    private List<CloverReportConfig> reports = newArrayList();

    /** size of cache used when loading per-test coverage */
    protected StorageSize coverageCacheSize = CoverageData.DEFAULT_EST_PER_TEST_COV_SIZE;


    public void addCurrent(CurrentEx current) {
        reports.add(current);
    }

    public void addHistorical(HistoricalEx historical) {
        reports.add(historical);
    }

    public void setCoverageCacheSize(String size) {
        try {
            if ("nocache".equals(size)) {
                coverageCacheSize = StorageSize.MAX;
            } else {
                coverageCacheSize = StorageSize.fromString(size);
            }
        } catch (IllegalArgumentException e) {
            Logger.getInstance().warn("Invalid coverage cache size value \"" + size + "\"; defaulting to " + coverageCacheSize.getSizeInBytes() + " bytes", e);
        }
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Generate the report.
     */
    @Override
    public void cloverExecute() {
        String initString = null;

        try {
            final Project p = getProject();
            final CloverReportConfig[] configs = new CloverReportConfig[reports.size()];
            final Map<String, CloverReportConfig> linkedReports = newHashMap();
            CloverReportConfig firstCurrentConfig = null;
            for (int i = 0; i < reports.size(); i++) {
                CloverReportConfig config = reports.get(i);
                config.setCoverageCacheSize(coverageCacheSize);
                if (config instanceof AntExtension) {
                    AntExtension extension = (AntExtension) config;
                    extension.resolve(p);
                    List<FileSet> reportFilesets = extension.getFilesets();
                    if (reportFilesets.size() != 0) {
                        // need to generate a reportFilter
                        config.setIncludeFilter(new FilesetFilter(p, reportFilesets));
                    }

                    config.setProjectName(projectName);

                    if (config instanceof CurrentEx) {
                        if (initString == null) {
                            initString = resolveInitString();
                        }
                        config.setInitString(initString);
                        log("Loading coverage database from: '" + initString + "'");                        
                        if (firstCurrentConfig == null && isHTML(config.getFormat())) {
                            firstCurrentConfig = config;
                        }

                        checkTestSourceFileSet(reportFilesets, (CurrentEx) config);
                    } else if (config instanceof HistoricalEx) {
                        HistoricalEx histEx = (HistoricalEx) config;

                        log("Loading historical coverage data from: '" + histEx.getHistoryDir() + "'");
                    }

                    // Do lazy qualification: ie. only append qualifier if required.
                    // use 'Current' or 'Historical' if title is null
                    final String title = (config.getTitle() == null) ? extension.getTypeName() :
                            (linkedReports.containsKey(config.getTitle())) ? // otherwise use title-type if title exists
                                    config.getTitle() + "-" + extension.getTypeName() : config.getTitle();

                    if (linkedReports.containsKey(title)) {
                        // add a qualifier
                        CloverReportConfig baseConfig = linkedReports.get(title);
                        config.setUniqueTitle(title + "-" + baseConfig.incTitleCount());
                    } else {
                        config.setUniqueTitle(title);
                    }

                }
                configs[i] = config;
                linkedReports.put(config.getUniqueTitle(), config);
            }
            generateReports(firstCurrentConfig, configs, linkedReports);
        } catch (CloverException e) {
            if (failOnError) {
                throw new BuildException(e);
            }
            else {
                log("Report generation failed: "+ e.getMessage(), Project.MSG_ERR);
            }
        }
    }

    private static boolean isHTML(Format format) {
        return format != null && format.in(Type.HTML);
    }

    static void checkTestSourceFileSet(List<FileSet> reportFilesets, CurrentEx current) throws CloverException {
        if (reportFilesets.size() > 0) {
            // ensure that all testsources are included in the model.
            final List<File> testFiles = current.getTestSourceFiles();
            final List<String> allFiles = current.getGlobalSourceFileNames();
            for (File file : testFiles) {
                if (!allFiles.contains(file.getAbsolutePath())) {
                    throw new CloverException("'" + file.getAbsolutePath() + "' is included in " +
                            "<testsources/>, but not in <fileset/>. " +
                            "<testsources/> must be a subset of <fileset/>");

                }
            }
        }
    }

    protected void generateReports(CloverReportConfig firstCurrentConfig, CloverReportConfig[] configs,
                                   Map<String, CloverReportConfig> linkedReports) throws CloverException {
        for (CloverReportConfig config : configs) {
            final Map<String, CloverReportConfig> myLinkedReports = newTreeMap();
            myLinkedReports.putAll(linkedReports); // copy all
            myLinkedReports.remove(config.getUniqueTitle()); // remove this report from the links

            config.setLinkedReports(myLinkedReports);
            config.setFirstCurrentConfig(firstCurrentConfig);
            CloverReporter.buildReporter(config).execute();
        }
    }

}
