package org.openclover.eclipse.core.settings;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.cfg.Interval;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.PluginVersionInfo;
import org.openclover.eclipse.core.views.CustomColumnDefinition;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import java.util.Collections;
import java.util.List;

import static clover.com.google.common.collect.Lists.newLinkedList;

public class InstallationSettings extends Settings {
    public static class Keys {
        public static final String COVERAGE_REFRESH_PERIOD = "coverage_refresh";
        public static final String ACTION_WHEN_REBUILDING = "action_when_rebuilding";
        public static final String WHEN_REBUILDING_PROMPT_ME = "when_rebuilding_prompt_me";
        public static final String ACTION_WHEN_CONTEXT_CHANGES = "action_when_context_changes";
        public static final String WHEN_CONTEXT_CHANGES_PROMPT_ME = "when_context_changes_prompt_me";
        public static final String ACTION_WHEN_INSTRUMENTATION_SOURCE_CHANGES = "action_when_instrumentation_source_changes";
        public static final String WHEN_INSTRUMENTATION_SOURCE_CHANGES_PROMPT_ME = "when_instrumentation_source_changes_prompt_me";
        public static final String AUTO_REFRESH_COVERAGE_DATA = "auto_refresh_coverage_data";
        public static final String COVERAGE_SPAN = "coverage_span";
        public static final String COVERAGE_STYLE_IN_EDITORS = "coverage_style_in_editors";
        public static final String WORKING_SET_ENABLED = "working_set_enabled";
        public static final String LOGGING_LEVEL = "logging_level";
        public static final String INSTALL_DATE = "install_date";
        public static final String PRIME_AWT_IN_UI_THREAD = "prime_awt_in_ui_thread";
        public static final String CUSTOM_COL_PREFIX = "custom.column.";
        public static final String CUSTOM_NAME_SUFFIX = ".name";
        public static final String CUSTOM_ABBREVIATED_NAME_SUFFIX = ".name.abbreviated";
        public static final String CUSTOM_EXPR_SUFFIX = ".expression";
        public static final String CUSTOM_STYLE_SUFFIX = ".style";
        public static final String CUSTOM_FORMAT_SUFFIX = ".format";
        public static final String CUSTOM_COL_COUNT = CUSTOM_COL_PREFIX + "count";
        public static final String AGGREGATE_COVERAGE = "aggregate_coverage";
        public static final String TRACK_PERTEST_COVERAGE = "track_pertest_coverage";
        public static final String PERTEST_COVERAGE_INMEM = "pertest_coverage_inmem";
        public static final String SHOW_EXCLUSION_ANNOTATIONS = "show_exclusion_annotations";
        /** Whether coverage should include hits from from failed tests as well */
        public static final String INCLUDE_FAILED_COVERAGE = "include_failed_coverage";
        /** Whether Clover should store instrumented sources in temporary directory */
        public static final String PRESERVE_INSTRUMENTED_SOURCES = "preserve_instrumented_sources";
        /** Whether Clover views shall be added to current perspective when user enables Clover on a project. */
        public static final String AUTO_OPEN_CLOVER_VIEWS = "auto_open_clover_views";
    }

    public static class Values {
        public static final long TWO_SECONDS_COVERAGE_REFRESH_PERIOD = 2000;
        public static final long FIVE_SECONDS_COVERAGE_REFRESH_PERIOD = 5000;
        public static final long TEN_SECONDS_COVERAGE_REFRESH_PERIOD = 10000;
        public static final long TWENTY_SECONDS_COVERAGE_REFRESH_PERIOD = 20000;
        public static final String WHEN_REBUILDING_CLEAR_COVERAGE = "clear";
        public static final String WHEN_REBUILDING_LEAVE_COVERAGE = "leave";
        public static final String WHEN_CONTEXT_CHANGES_REBUILD = "rebuild";
        public static final String WHEN_CONTEXT_CHANGES_IGNORE = "ignore";
        public static final int SHOW_ALL_COVERAGE_IN_EDITORS = 0;
        public static final int SHOW_ONLY_UNCOVERED_IN_EDITORS = 1;
        public static final int SHOW_NO_COVERAGE_IN_EDITORS = 2;
        public static final String NO_LOGGING_LEVEL = "none";
        public static final String DEBUG_LOGGING_LEVEL = "debug";
        public static final String VERBOSE_LOGGING_LEVEL = "verbose";
        public static final String INFO_LOGGING_LEVEL = "info";
    }

    public static class Defaults {
        public static final long COVERAGE_REFRESH_PERIOD = Values.TWO_SECONDS_COVERAGE_REFRESH_PERIOD;
        public static final boolean REFRESH_COVERAGE = true;
        public static final String ACTION_WHEN_REBUILDING = Values.WHEN_REBUILDING_CLEAR_COVERAGE;
        public static final boolean WHEN_REBUILDING_PROMPT_ME = true;
        public static final String ACTION_WHEN_CONTEXT_CHANGES = Values.WHEN_CONTEXT_CHANGES_IGNORE;
        public static final boolean WHEN_CONTEXT_CHANGE_PROMPT_ME = true;
        public static final String ACTION_WHEN_INSTRUMENTATION_SOURCE_CHANGES = Values.WHEN_CONTEXT_CHANGES_IGNORE;
        public static final boolean WHEN_INSTRUMENTATION_SOURCE_CHANGE_PROMPT_ME = true;
        public static final String COVERAGE_SPAN = "0s";
        public static final int SHOW_COVERAGE_IN_EDITORS = Values.SHOW_ALL_COVERAGE_IN_EDITORS;
        public static final String LOGGING_LEVEL = Values.NO_LOGGING_LEVEL;
        public static final boolean PRIME_AWT_IN_UI_THREAD = false;
        public static final boolean ALWAYS_FULL_BUILD = false;
        public static final boolean AGGREGATE_COVERAGE = true;
        public static final boolean TRACK_PERTEST_COVERAGE = true;
        public static final boolean PERTEST_COVERAGE_INMEM = false;
        public static final boolean SHOW_EXCLUSION_ANNOTATIONS = true;
        /** True - coverage from passed and failed tests is considered, false - only coverage from passed tests is considered */
        public static final boolean INCLUDE_FAILED_COVERAGE = false;
        /** True - every file being instrumented will be stored on disk, false - not */
        public static final boolean PRESERVE_INSTRUMENTED_SOURCES = false;
        /** True - Clover views will be opened when user enables Clover on a project */
        public static final boolean AUTO_OPEN_CLOVER_VIEWS = true;
    }

    private List<CustomColumnDefinition> customColumns;

    public InstallationSettings() {
        isolatedPreferences = (IEclipsePreferences)Platform.getPreferencesService().getRootNode().node(ConfigurationScope.SCOPE).node(CloverPlugin.ID);
        load();
    }

    public void setDebugging(boolean debug) {
        setValue(
            Keys.LOGGING_LEVEL,
            debug
                ? Values.DEBUG_LOGGING_LEVEL
                : Values.NO_LOGGING_LEVEL);
    }

    public long getCoverageRefreshPeriod() {
        return getLong(Keys.COVERAGE_REFRESH_PERIOD, 0l);
    }

    public boolean isAutoRefreshingCoverage() {
        return getBoolean(Keys.AUTO_REFRESH_COVERAGE_DATA);
    }

    public void setAutoRefreshingCoverage(boolean autoRefresh) {
        setValue(Keys.AUTO_REFRESH_COVERAGE_DATA, autoRefresh);
    }

    public String getLoggingLevel() {
        return getString(Keys.LOGGING_LEVEL);
    }

    public boolean isDeletingCoverageOnRebuild() {
        return Values.WHEN_REBUILDING_CLEAR_COVERAGE.equals(
            getString(Keys.ACTION_WHEN_REBUILDING));
    }

    public boolean isPromptingOnRebuild() {
        return getBoolean(Keys.WHEN_REBUILDING_PROMPT_ME);
    }

    public void setLatestPromptOnRebuildDecision(boolean coverageCleared) {
        setValue(
            Keys.ACTION_WHEN_REBUILDING,
            coverageCleared
                ? Values.WHEN_REBUILDING_CLEAR_COVERAGE
                : Values.WHEN_REBUILDING_LEAVE_COVERAGE);
    }

    public void setPromptingOnRebuild(boolean prompt) {
        setValue(Keys.WHEN_REBUILDING_PROMPT_ME, prompt);
    }

    public Interval getCoverageSpan() {
        try {
            String spanDescription = getString(Keys.COVERAGE_SPAN);
            return new Interval(spanDescription == null ? Defaults.COVERAGE_SPAN : spanDescription);
        } catch (NumberFormatException e) {
            return new Interval(Defaults.COVERAGE_SPAN);
        }
    }

    public boolean isAggregatingCoverage() {
        return getBoolean(Keys.AGGREGATE_COVERAGE);
    }

    public void setAggregateCoverage(boolean aggregate) {
        setValue(Keys.AGGREGATE_COVERAGE, aggregate);
    }

    public boolean isTrackingPerTestCoverage() {
        return getBoolean(Keys.TRACK_PERTEST_COVERAGE);
    }

    public void setTrackingPerTestCoverage(boolean track) {
        setValue(Keys.TRACK_PERTEST_COVERAGE, track);
    }

    public boolean isPerTestCoverageInMemory() {
        return getBoolean(Keys.PERTEST_COVERAGE_INMEM);
    }

    public void setPerTestCoverageInMemory(boolean inMemory) {
        setValue(Keys.PERTEST_COVERAGE_INMEM, inMemory);
    }

    public long getCoverageSpanInMillis() {
        return getCoverageSpan().getValueInMillis();
    }

    public boolean isRebuildingOnContextChange() {
        return Values.WHEN_CONTEXT_CHANGES_REBUILD.equals(
            getString(Keys.ACTION_WHEN_CONTEXT_CHANGES));
    }

    public boolean isPromptingOnContextChange() {
        return getBoolean(Keys.WHEN_CONTEXT_CHANGES_PROMPT_ME);
    }

    public void setLatestPromptOnContextChange(boolean rebuild) {
        setValue(
            Keys.ACTION_WHEN_CONTEXT_CHANGES,
            rebuild
                ? Values.WHEN_CONTEXT_CHANGES_REBUILD
                : Values.WHEN_CONTEXT_CHANGES_IGNORE);
    }

    public void setPromptingOnContextChange(boolean prompt) {
        setValue(Keys.WHEN_CONTEXT_CHANGES_PROMPT_ME, prompt);
    }

    public boolean isRebuildingOnInstrumentationSourceChange() {
        return Values.WHEN_CONTEXT_CHANGES_REBUILD.equals(
            getString(Keys.ACTION_WHEN_INSTRUMENTATION_SOURCE_CHANGES));
    }

    public boolean isPromptingOnInstrumentationSourceChange() {
        return getBoolean(Keys.WHEN_INSTRUMENTATION_SOURCE_CHANGES_PROMPT_ME);
    }

    public void setLatestPromptOnInstrumentationSourceChange(boolean rebuild) {
        setValue(
            Keys.ACTION_WHEN_INSTRUMENTATION_SOURCE_CHANGES,
            rebuild
                ? Values.WHEN_CONTEXT_CHANGES_REBUILD
                : Values.WHEN_CONTEXT_CHANGES_IGNORE);
    }

    public void isPromptingOnInstrumentationSourceChange(boolean prompt) {
        setValue(Keys.WHEN_INSTRUMENTATION_SOURCE_CHANGES_PROMPT_ME, prompt);
    }

    public List<CustomColumnDefinition> getCustomColumns() {
        if (customColumns == null) {
            customColumns = loadCustomColumns();
        }
        return customColumns;
    }

    public void setCustomColumns(List<CustomColumnDefinition> columns) {
        setValue(Keys.CUSTOM_COL_COUNT, columns.size());
        for (int i = 0; i < columns.size(); i++) {
            CustomColumnDefinition columnDef = columns.get(i);
            setValue(
                Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_NAME_SUFFIX,
                columnDef.getTitle());
            setValue(
                Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_ABBREVIATED_NAME_SUFFIX,
                columnDef.getAbbreviatedTitle());
            setValue(
                Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_EXPR_SUFFIX,
                columnDef.getExpression());
        }
        customColumns = columns;
    }

    private List<CustomColumnDefinition> loadCustomColumns() {
        List<CustomColumnDefinition> customColumns = newLinkedList();
        int customColCount = getInt(Keys.CUSTOM_COL_COUNT);
        if (customColCount > 0) {
            for (int i = 0; i < customColCount; i++) {
                String title = getString(Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_NAME_SUFFIX);
                String abbreviatedTitle = getString(Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_ABBREVIATED_NAME_SUFFIX);
                abbreviatedTitle = abbreviatedTitle == null ? title : abbreviatedTitle;
                String expression = getString(Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_EXPR_SUFFIX);
                int style = getInt(Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_STYLE_SUFFIX);
                int format = getInt(Keys.CUSTOM_COL_PREFIX + i + Keys.CUSTOM_FORMAT_SUFFIX);
                try {
                    customColumns.add(new CustomColumnDefinition(title, abbreviatedTitle, expression, style, format));
                } catch (CloverException e) {
                    CloverPlugin.logError("Unable to define load custom column \"" + title + "\" : \"" + expression + "\" : \"" + e.getMessage() + "\" - ignoring", e);
                }
            }
        }
        return Collections.unmodifiableList(customColumns);
    }

    public int getEditorCoverageStyle() {
        return getInt(Keys.COVERAGE_STYLE_IN_EDITORS);
    }

    public void setEditorCoverageStyle(int style) {
        setValue(Keys.COVERAGE_STYLE_IN_EDITORS, style);
    }

    public long getInstallDate() {
        return getLong(Keys.INSTALL_DATE);
    }

    public void setInstallDate(long when) {
        setValue(Keys.INSTALL_DATE, when);
    }

    public boolean isShowExclusionAnnotations() {
        return getBoolean(Keys.SHOW_EXCLUSION_ANNOTATIONS);
    }

    public void setShowExclusionAnnotations(boolean show) {
        setValue(Keys.SHOW_EXCLUSION_ANNOTATIONS, show);
    }

    public boolean isIncludeFailedCoverage() {
        return getBoolean(Keys.INCLUDE_FAILED_COVERAGE);
    }

    public void setIncludeFailedCoverage(boolean include) {
        setValue(Keys.INCLUDE_FAILED_COVERAGE, include);
    }

    public boolean isPreserveInstrumentedSources() {
        return getBoolean(Keys.PRESERVE_INSTRUMENTED_SOURCES);
    }

    public void setPreserveInstrumentedSources(boolean preserve) {
        setValue(Keys.PRESERVE_INSTRUMENTED_SOURCES, preserve);
    }

    public boolean isAutoOpenCloverViews() {
        return getBoolean(Keys.AUTO_OPEN_CLOVER_VIEWS);
    }

    public void setAutoOpenCloverViews(boolean autoOpen) {
        setValue(Keys.AUTO_OPEN_CLOVER_VIEWS, autoOpen);
    }
}
