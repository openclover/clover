package org.openclover.idea.config;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.util.ModelScope;
import org.openclover.idea.util.ProjectUtil;

import java.io.File;
import java.util.Map;

public class IdeaCloverConfig extends MappedCloverPluginConfig {

    // extra keys for storing values related with IDEA IDE

    public static final String AUTO_SCROLL_TO_SOURCE = "autoScroll";
    public static final String AUTO_SCROLL_FROM_SOURCE = "autoScrollFromSource";
    public static final String FLATTEN_PACKAGES = "flattenPackages";

    public static final String SHOW_SUMMARY_IN_TOOLBAR = "showSummaryInToolbar";
    public static final String SHOW_SUMMARY_IN_TOOLWINDOW = "showSummaryInToolwindow";
    public static final String SHOW_ERROR_MARKS = "showErrorMarks";

    public static final String ALWAYS_EXPAND_TEST_CLASSES = "alwaysExpandTestClasses";
    public static final String ALWAYS_COLLAPSE_TEST_CLASSES = "alwaysCollapseTestClasses";

    public static final String HIGHLIGHT_COVERED = "highlightCovered";

    public static final String LAST_PROJECT_CONFIG_TAB_SELECTION = "lastProjectConfigTabSelection";
    public static final String MODEL_SCOPE = "modelScope";
    public static final String INCLUDE_PASSED_TEST_COVERAGE_ONLY = "includePassedTestCoverageOnly";
    public static final String TEST_VIEW_SCOPE = "testViewScope";
    public static final String TEST_CASE_LAYOUT = "testCaseLayout";
    public static final String CALCULATE_TEST_COVERAGE = "calculateTestCoverage";
    public static final String HIDE_FULLY_COVERED = "hideFullyCovered";

    public static final String VIEW_COVERAGE = "viewCoverage";
    public static final String VIEW_INCLUDE_ANNOTATION = "viewIncludeAnnotation";

    public static final String CLOUD_REPORT_INCLUDE_SUBPKGS = "cloudReportIncludeSubpkgs";
    public static final String CLOUD_AUTO_VIEW = "cloudAutoView";

    // default values for keys

    private static final boolean DEFAULT_SHOW_ERROR_MARKS = true;
    private static final boolean DEFAULT_SHOW_SUMMARY_IN_TOOLWINDOW = true;
    private static final boolean DEFAULT_SHOW_SUMMARY_IN_TOOLBAR = true;
    private static final boolean DEFAULT_FLATTEN_PACKAGES = false;
    private static final boolean DEFAULT_AUTO_SCROLL_TO_SOURCE = true;
    private static final boolean DEFAULT_AUTO_SCROLL_FROM_SOURCE = false;

    private static final boolean DEFAULT_ALWAYS_EXPAND_TEST_CLASSES = false;
    private static final boolean DEFAULT_ALWAYS_COLLAPSE_TEST_CLASSES = false;

    private static final boolean DEFAULT_HIGHLIGHT_COVERED = true;

    private static final int DEFAULT_LAST_PROJECT_CONFIG_TAB_SELECTION = 0;

    private static final ModelScope DEFAULT_MODEL_SCOPE = ModelScope.APP_CLASSES_ONLY;
    private static final boolean DEFAULT_INCLUDE_PASSED_TEST_COVERAGE_ONLY = true;
    private static final TestCaseLayout DEFAULT_TEST_CASE_LAYOUT = TestCaseLayout.TEST_CASES;
    private static final TestViewScope DEFAULT_TEST_VIEW_SCOPE = TestViewScope.GLOBAL;
    private static final boolean DEFAULT_CALCULATE_TEST_COVERAGE = true;
    private static final boolean DEFAULT_HIDE_FULLY_COVERED = false;

    private static final boolean DEFAULT_VIEW_COVERAGE = true;
    private static final boolean DEFAULT_VIEW_INCLUDE_ANNOTATION = true;

    private static final boolean DEFAULT_CLOUD_REPORT_INCLUDE_SUBPKGS = false;
    private static final boolean DEFAULT_CLOUD_AUTO_VIEW = true;


    private final Project project;


    public static IdeaCloverConfig fromProject(Project project) {
        return new IdeaCloverConfig(project);
    }

    private IdeaCloverConfig(Project project) {
        this.project = project;
        setDefaults(properties);
        calculateAndStoreGeneratedInitString(properties, project);
    }

    /**
     * Calculate value of the initstring in case when user selects "use default location". This location
     * can be calculated on the IDE side. In the external build process it shall be just read from the
     * property map.
     * @param properties
     * @param project
     */
    private void calculateAndStoreGeneratedInitString(Map<String,Object> properties,  Project project) {
        if (project != null) {
            final File projectWorkspace = ProjectUtil.getProjectWorkspace(project);
            final File coverageDb = new File(projectWorkspace, "coverage.db");
            properties.put(GENERATED_INIT_STRING, coverageDb.getAbsolutePath());
        } else {
            // still try to put something
            properties.put(GENERATED_INIT_STRING, DEFAULT_GENERATED_INIT_STRING);
        }
    }

    /**
     * Make a data only copy of this config object.
     *
     * @param config target to copy this configuration to. If null, new config is created.
     * @return config copy
     */
    public IdeaCloverConfig copyConfigTo(IdeaCloverConfig config) {
        final IdeaCloverConfig theConfig = config != null ? config : new IdeaCloverConfig(project);

        theConfig.properties.clear();
        theConfig.properties.putAll(this.properties);

        return theConfig;
    }

    @Override
    protected void setDefaults(Map<String, Object> propertyMap) {
        // call super to store instrumenter properties
        super.setDefaults(propertyMap);

        // store more stuff related with IDEA; mind the autoboxing in most of the statements below
        propertyMap.put(SHOW_ERROR_MARKS, DEFAULT_SHOW_ERROR_MARKS);
        propertyMap.put(SHOW_SUMMARY_IN_TOOLBAR, DEFAULT_SHOW_SUMMARY_IN_TOOLBAR);
        propertyMap.put(SHOW_SUMMARY_IN_TOOLWINDOW, DEFAULT_SHOW_SUMMARY_IN_TOOLWINDOW);
        propertyMap.put(FLATTEN_PACKAGES, DEFAULT_FLATTEN_PACKAGES);
        propertyMap.put(AUTO_SCROLL_TO_SOURCE, DEFAULT_AUTO_SCROLL_TO_SOURCE);
        propertyMap.put(AUTO_SCROLL_FROM_SOURCE, DEFAULT_AUTO_SCROLL_FROM_SOURCE);
        propertyMap.put(INSTRUMENT_TESTS, DEFAULT_INSTRUMENT_TESTS);
        propertyMap.put(ALWAYS_COLLAPSE_TEST_CLASSES, DEFAULT_ALWAYS_COLLAPSE_TEST_CLASSES);
        propertyMap.put(ALWAYS_EXPAND_TEST_CLASSES, DEFAULT_ALWAYS_EXPAND_TEST_CLASSES);
        propertyMap.put(HIGHLIGHT_COVERED, DEFAULT_HIGHLIGHT_COVERED);
        propertyMap.put(LAST_PROJECT_CONFIG_TAB_SELECTION, DEFAULT_LAST_PROJECT_CONFIG_TAB_SELECTION);
        propertyMap.put(VIEW_COVERAGE, DEFAULT_VIEW_COVERAGE);
        propertyMap.put(VIEW_INCLUDE_ANNOTATION, DEFAULT_VIEW_INCLUDE_ANNOTATION);
        propertyMap.put(CALCULATE_TEST_COVERAGE, DEFAULT_CALCULATE_TEST_COVERAGE);
        propertyMap.put(HIDE_FULLY_COVERED, DEFAULT_HIDE_FULLY_COVERED);

        propertyMap.put(MODEL_SCOPE, DEFAULT_MODEL_SCOPE);
        propertyMap.put(INCLUDE_PASSED_TEST_COVERAGE_ONLY, DEFAULT_INCLUDE_PASSED_TEST_COVERAGE_ONLY);
        propertyMap.put(TEST_VIEW_SCOPE, DEFAULT_TEST_VIEW_SCOPE);
        propertyMap.put(TEST_CASE_LAYOUT, DEFAULT_TEST_CASE_LAYOUT);
        propertyMap.put(CLOUD_REPORT_INCLUDE_SUBPKGS, DEFAULT_CLOUD_REPORT_INCLUDE_SUBPKGS);
    }

    public void setAutoScroll(boolean b) {
        putProperty(AUTO_SCROLL_TO_SOURCE, b);
    }

    public boolean isAutoScroll() {
        return getBooleanProperty(AUTO_SCROLL_TO_SOURCE, DEFAULT_AUTO_SCROLL_TO_SOURCE);
    }

    public void setAutoScrollFromSource(boolean b) {
        putProperty(AUTO_SCROLL_FROM_SOURCE, b);
    }

    public boolean isAutoScrollFromSource() {
        return getBooleanProperty(AUTO_SCROLL_FROM_SOURCE, DEFAULT_AUTO_SCROLL_FROM_SOURCE);
    }

    public void setFlattenPackages(boolean b) {
        putProperty(FLATTEN_PACKAGES, b);
    }

    public boolean isFlattenPackages() {
        return getBooleanProperty(FLATTEN_PACKAGES, DEFAULT_FLATTEN_PACKAGES);
    }

    public void setShowSummaryInToolbar(boolean b) {
        putProperty(SHOW_SUMMARY_IN_TOOLBAR, b);
    }

    public boolean isShowSummaryInToolbar() {
        return getBooleanProperty(SHOW_SUMMARY_IN_TOOLBAR, DEFAULT_SHOW_SUMMARY_IN_TOOLBAR);
    }

    public void setShowSummaryInToolwindow(boolean b) {
        putProperty(SHOW_SUMMARY_IN_TOOLWINDOW, b);
    }

    public boolean isShowSummaryInToolwindow() {
        return getBooleanProperty(SHOW_SUMMARY_IN_TOOLWINDOW, DEFAULT_SHOW_SUMMARY_IN_TOOLWINDOW);
    }

    public void setShowErrorMarks(boolean b) {
        putProperty(SHOW_ERROR_MARKS, b);
    }

    public boolean isShowErrorMarks() {
        return getBooleanProperty(SHOW_ERROR_MARKS, DEFAULT_SHOW_ERROR_MARKS);
    }

    public void setAlwaysExpandTestClasses(boolean b) {
        if (b) {
            setAlwaysCollapseTestClasses(false);
        }
        putProperty(ALWAYS_EXPAND_TEST_CLASSES, b);
    }

    public boolean isAlwaysExpandTestClasses() {
        return getBooleanProperty(ALWAYS_EXPAND_TEST_CLASSES, DEFAULT_ALWAYS_EXPAND_TEST_CLASSES);
    }

    public void setAlwaysCollapseTestClasses(boolean b) {
        if (b) {
            setAlwaysExpandTestClasses(false);
        }
        putProperty(ALWAYS_COLLAPSE_TEST_CLASSES, b);
    }

    public boolean isAlwaysCollapseTestClasses() {
        return getBooleanProperty(ALWAYS_COLLAPSE_TEST_CLASSES, DEFAULT_ALWAYS_COLLAPSE_TEST_CLASSES);
    }

    public void setHighlightCovered(boolean b) {
        putProperty(HIGHLIGHT_COVERED, b);
    }

    public boolean isHighlightCovered() {
        return getBooleanProperty(HIGHLIGHT_COVERED, DEFAULT_HIGHLIGHT_COVERED);
    }

    public void setLastProjectConfigTabSelected(int selection) {
        putProperty(LAST_PROJECT_CONFIG_TAB_SELECTION, selection);
    }

    public int getLastProjectConfigTabSelected() {
        return getIntProperty(LAST_PROJECT_CONFIG_TAB_SELECTION, DEFAULT_LAST_PROJECT_CONFIG_TAB_SELECTION);
    }

    @NotNull
    public ModelScope getModelScope() {
        return (ModelScope) getProperty(MODEL_SCOPE, DEFAULT_MODEL_SCOPE);
    }

    public void setModelScope(@NotNull ModelScope modelScope) {
        putProperty(MODEL_SCOPE, modelScope);
    }

    public boolean isIncludePassedTestCoverageOnly() {
        return getBooleanProperty(INCLUDE_PASSED_TEST_COVERAGE_ONLY, DEFAULT_INCLUDE_PASSED_TEST_COVERAGE_ONLY);
    }

    public void setIncludePassedTestCoverageOnly(boolean includePassedTestCoverageOnly) {
        putProperty(INCLUDE_PASSED_TEST_COVERAGE_ONLY, includePassedTestCoverageOnly);
    }

    @NotNull
    public TestCaseLayout getTestCaseLayout() {
        return (TestCaseLayout) getProperty(TEST_CASE_LAYOUT, DEFAULT_TEST_CASE_LAYOUT);
    }

    public void setTestCaseLayout(TestCaseLayout testCaseLayout) {
        putProperty(TEST_CASE_LAYOUT, testCaseLayout);
    }

    public boolean isViewCoverage() {
        return getBooleanProperty(VIEW_COVERAGE, DEFAULT_VIEW_COVERAGE);
    }

    public void setViewCoverage(boolean viewCoverage) {
        putProperty(VIEW_COVERAGE, viewCoverage);
    }

    public boolean isViewIncludeAnnotation() {
        return getBooleanProperty(VIEW_INCLUDE_ANNOTATION, DEFAULT_VIEW_INCLUDE_ANNOTATION);
    }

    public void setViewIncludeAnnotation(boolean viewIncludeAnnotation) {
        putProperty(VIEW_INCLUDE_ANNOTATION, viewIncludeAnnotation);
    }

    @NotNull
    public TestViewScope getTestViewScope() {
        return (TestViewScope) getProperty(TEST_VIEW_SCOPE, DEFAULT_TEST_VIEW_SCOPE);
    }

    public void setTestViewScope(TestViewScope scope) {
        putProperty(TEST_VIEW_SCOPE, scope);
    }

    public boolean isCalculateTestCoverage() {
        return getBooleanProperty(CALCULATE_TEST_COVERAGE, DEFAULT_CALCULATE_TEST_COVERAGE);
    }

    public void setCalculateTestCoverage(boolean state) {
        putProperty(CALCULATE_TEST_COVERAGE, state);
    }

    public boolean isHideFullyCovered() {
        return getBooleanProperty(HIDE_FULLY_COVERED, DEFAULT_HIDE_FULLY_COVERED);
    }

    public void setHideFullyCovered(boolean hideFullyCovered) {
        putProperty(HIDE_FULLY_COVERED, hideFullyCovered);
    }

    public boolean isCloudReportIncludeSubpkgs() {
        return getBooleanProperty(CLOUD_REPORT_INCLUDE_SUBPKGS, DEFAULT_CLOUD_REPORT_INCLUDE_SUBPKGS);
    }

    public void setCloudReportIncludeSubpkgs(boolean include) {
        putProperty(CLOUD_REPORT_INCLUDE_SUBPKGS, include);
    }

    public boolean isAutoViewInCloudReport() {
        return getBooleanProperty(CLOUD_AUTO_VIEW, DEFAULT_CLOUD_AUTO_VIEW);
    }

    public void setAutoViewInCloudReport(boolean auto) {
        putProperty(CLOUD_AUTO_VIEW, auto);
    }

}
