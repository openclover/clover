package com.atlassian.clover.idea.config;

import com.atlassian.clover.CloverLicenseInfo;
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import com.atlassian.clover.idea.config.regexp.Regexp;
import com.atlassian.clover.util.collections.Pair;
import org.jetbrains.annotations.Nullable;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.List;
import java.util.Map;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Maps.newHashMap;

/**
 * Simple implementation of the CloverPluginConfig interface, using a in memory Map
 * as the persistant store for property values.
 */
public class MappedCloverPluginConfig extends AbstractCloverPluginConfig {

    // keys for a map
    public static final String ENABLED = "enabled";

    public static final String MANUAL_INIT_STRING = "manualInitString";
    public static final String AUTO_INIT_STRING = "automaticInitString";
    public static final String RELATIVE_INIT_STRING = "relativeInitString";

    public static final String FLUSH_POLICY = "flushPolicy";
    public static final String FLUSH_INTERVAL = "flushInterval";
    public static final String SPAN = "span";
    public static final String INCLUDES = "includes";
    public static final String EXCLUDES = "excludes";

    public static final String CONTEXT = "context";

    public static final String SHOW_GUTTER = "showGutter";
    public static final String SHOW_TOOLTIPS = "showTooltips";
    public static final String SHOW_INLINE = "showInline";

    public static final String AUTO_REFRESH = "autoRefresh";
    public static final String PERIODIC_REFRESH = "periodicRefresh";
    public static final String AUTO_REFRESH_INTERVAL = "autoRefreshInterval";

    public static final String REGEXP_CONTEXTS = "regexpContexts";

    public static final String COVERED_STRIPE = "coveredStripe";
    public static final String COVERED_HIGHLIGHT = "coveredHighlight";
    public static final String FAILED_COVERED_STRIPE = "failedCoveredStripe";
    public static final String FAILED_COVERED_HIGHLIGHT = "failedCoveredHighlight";
    public static final String NOT_COVERED_STRIPE = "notCoveredStripe";
    public static final String NOT_COVERED_HIGHLIGHT = "notCoveredHighlight";
    public static final String OUTOFDATE_STRIPE = "outOfDateStripe";
    public static final String OUTOFDATE_HIGHLIGHT = "outOfDateHighlight";
    public static final String FILTERED_STRIPE = "filteredStripe";
    public static final String FILTERED_HIGHLIGHT = "filteredHighlight";

    public static final String LOAD_PER_TEST_DATA = "loadPerTestData";
    public static final String PROJECT_REBUILD = "projectRebuild";

    public static final String LANGUAGE_LEVEL = "languageLevel";

    public static final String GENERATED_INIT_STRING = "generatedInitString";
    public static final String BUILD_WITH_CLOVER = "buildWithClover";
    public static final String INSTRUMENT_TESTS = "instrumentTests";
    public static final String INSTRUMENT_LAMBDA = "instrumentLambda";

    // for debug purposes: whether to dump instrumented version of sources into temporary directory
    public static final String DUMP_INSTRUMENTED_SOURCES = "dumpInstrumentedSources";

    // default values for keys
    public static final boolean DEFAULT_AUTO_INIT_STRING = true;
    public static final boolean DEFAULT_RELATIVE_INIT_STRING = false;
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_FLUSH_POLICY = DIRECTED_FLUSHING;
    public static final int DEFAULT_FLUSH_INTERVAL = 5000;
    public static final String DEFAULT_SPAN = "0";
    public static final String DEFAULT_CONTEXT = "";
    public static final boolean DEFAULT_SHOW_GUTTER = true;
    public static final boolean DEFAULT_SHOW_INLINE = true;
    public static final boolean DEFAULT_SHOW_TOOLTIPS = true;
    public static final boolean DEFAULT_AUTO_REFRESH = true;
    public static final boolean DEFAULT_PERIODIC_REFRESH = false;
    public static final int DEFAULT_AUTO_REFRESH_INTERVAL = 2000;

    public static final List DEFAULT_REGEXP_CONTEXTS = newArrayList();

    // default highlighting colours for bright schemes (Alloy, IntelliJ, Windows)
    public static final Color DEFAULT_LIGHT_COVERED_STRIPE = Color.GREEN;
    public static final Color DEFAULT_LIGHT_COVERED_HIGHLIGHT = new Color(0xC8FAC8);
    public static final Color DEFAULT_LIGHT_FAILED_COVERED_STRIPE = Color.ORANGE;
    public static final Color DEFAULT_LIGHT_FAILED_COVERED_HIGHLIGHT = new Color(0xfff0b0);
    public static final Color DEFAULT_LIGHT_NOT_COVERED_STRIPE = Color.RED;
    public static final Color DEFAULT_LIGHT_NOT_COVERED_HIGHLIGHT = new Color(0xFAC8C8);
    public static final Color DEFAULT_LIGHT_OUTOFDATE_STRIPE = Color.YELLOW;
    public static final Color DEFAULT_LIGHT_OUTOFDATE_HIGHLIGHT = new Color(0xFFFFE0);
    public static final Color DEFAULT_LIGHT_FILTERED_STRIPE = Color.DARK_GRAY;
    public static final Color DEFAULT_LIGHT_FILTERED_HIGHLIGHT = Color.LIGHT_GRAY;

    // default highlighting colours for Darcula schemes
    public static final Color DEFAULT_DARCULA_COVERED_STRIPE = Color.GREEN;
    public static final Color DEFAULT_DARCULA_COVERED_HIGHLIGHT = new Color(0, 80, 0);
    public static final Color DEFAULT_DARCULA_FAILED_COVERED_STRIPE = Color.ORANGE;
    public static final Color DEFAULT_DARCULA_FAILED_COVERED_HIGHLIGHT = new Color(80, 80, 0);
    public static final Color DEFAULT_DARCULA_NOT_COVERED_STRIPE = Color.RED;
    public static final Color DEFAULT_DARCULA_NOT_COVERED_HIGHLIGHT = new Color(80, 0, 0);
    public static final Color DEFAULT_DARCULA_OUTOFDATE_STRIPE = Color.YELLOW;
    public static final Color DEFAULT_DARCULA_OUTOFDATE_HIGHLIGHT = new Color(0x00000F);
    public static final Color DEFAULT_DARCULA_FILTERED_STRIPE = Color.LIGHT_GRAY;
    public static final Color DEFAULT_DARCULA_FILTERED_HIGHLIGHT = new Color(80, 80, 80);

    public static final boolean DEFAULT_LOAD_PER_TEST_DATA = true;
    public static final ProjectRebuild DEFAULT_PROJECT_REBUILD = ProjectRebuild.ASK;

    public static final String DEFAULT_GENERATED_INIT_STRING = "coverage.db";
    public static final boolean DEFAULT_BUILD_WITH_CLOVER = true;
    public static final boolean DEFAULT_INSTRUMENT_TESTS = true;
    public static final boolean DEFAULT_DUMP_INSTRUMENTED_SOURCES = false;
    public static final LambdaInstrumentation DEFAULT_INSTRUMENT_LAMBDA = LambdaInstrumentation.NONE;

    protected final Map<String, Object> properties = newHashMap();

    @Override
    public void writeProperty(String name, Object value) {
        properties.put(name, value);
    }

    @Override
    @Nullable
    public Object readProperty(String name) {
        return properties.get(name);
    }

    public Color getColorProperty(String name) {
        Pair colorPair = (Pair)getProperty(name);
        return isUnderDarcula() ? (Color)colorPair.second : (Color)colorPair.first;
    }

    public void putColorProperty(String name, Color color) {
        Pair colourPair = (Pair)getProperty(name);
        if (colourPair != null) {
            // update only one color (light or darcula) in existing pair
            putProperty(name,
                    isUnderDarcula() ? Pair.of(colourPair.first, color) : Pair.of(color, colourPair.second));
        } else {
            // this can be a case for CloverJpsProjectConfigurationSerializer
            putProperty(name, Pair.of(color, color));
        }
    }

    private static boolean isUnderDarcula() {
        // note: com.intellij.util.ui.UIUtil.isUnderDarcula() became available since IDEA12
        return UIManager.getLookAndFeel().getName().contains("Darcula");
    }

    /**
     * Get the configured init string.
     */
    @Override
    public String getInitString() {
        if (getUseGeneratedInitString()) {
            return getGeneratedInitString();
        }
        return getManualInitString();
    }

    @SuppressWarnings("unused") // used for bean<->xml serialization
    public void setGeneratedInitString(String generatedInitString) {
        putProperty(GENERATED_INIT_STRING, generatedInitString);
    }

    /**
     * This method should be overridden by IDE specific implementations to
     * provide functionality for generating IDE specific init strings.
     */
    @Override
    public String getGeneratedInitString() {
        return (String)getProperty(GENERATED_INIT_STRING, DEFAULT_GENERATED_INIT_STRING);
    }

    @Override
    public String getManualInitString() {
        return (String) getProperty(MANUAL_INIT_STRING);
    }

    @Override
    public void setManualInitString(String str) {
        putProperty(MANUAL_INIT_STRING, str);
    }

    @Override
    public void setUseGeneratedInitString(boolean b) {
        putProperty(AUTO_INIT_STRING, b);
    }

    @Override
    public boolean getUseGeneratedInitString() {
        return getBooleanProperty(AUTO_INIT_STRING, DEFAULT_AUTO_INIT_STRING);
    }

    @Override
    public boolean isRelativeInitString() {
        return getBooleanProperty(RELATIVE_INIT_STRING, DEFAULT_RELATIVE_INIT_STRING);
    }

    @Override
    public void setRelativeInitString(boolean b) {
        putProperty(RELATIVE_INIT_STRING, b);
    }

    /**
     * Allows enabling the plugin only when current license has not terminated.
     *
     * @param enabled enable plugin
     */
    @Override
    public void setEnabled(boolean enabled) {
        putProperty(ENABLED, enabled && !CloverLicenseInfo.TERMINATED);
    }

    @Override
    public boolean isEnabled() {
        return getBooleanProperty(ENABLED, DEFAULT_ENABLED) && !CloverLicenseInfo.TERMINATED;
    }

    @Override
    public void setFlushPolicy(int i) {
        putProperty(FLUSH_POLICY, i);
    }

    @Override
    public int getFlushPolicy() {
        return getIntProperty(FLUSH_POLICY, DEFAULT_FLUSH_POLICY);
    }

    @Override
    public void setFlushInterval(int interval) {
        putProperty(FLUSH_INTERVAL, interval);
    }

    @Override
    public int getFlushInterval() {
        return getIntProperty(FLUSH_INTERVAL, DEFAULT_FLUSH_INTERVAL);
    }

    @Override
    public void setIncludes(String includes) {
        putProperty(INCLUDES, includes);
    }

    @Override
    public String getIncludes() {
        return (String) getProperty(INCLUDES);
    }

    @Override
    public void setExcludes(String excludes) {
        putProperty(EXCLUDES, excludes);
    }

    @Override
    public String getExcludes() {
        return (String) getProperty(EXCLUDES);
    }

    @Override
    public void setContextFilterSpec(String ctx) {
        putProperty(CONTEXT, ctx);
    }

    @Override
    public String getContextFilterSpec() {
        return (String) getProperty(CONTEXT, DEFAULT_CONTEXT);
    }

    @Override
    public void setShowGutter(boolean b) {
        putProperty(SHOW_GUTTER, b);
    }

    @Override
    public boolean isShowGutter() {
        return getBooleanProperty(SHOW_GUTTER, DEFAULT_SHOW_GUTTER);
    }

    @Override
    public void setShowInline(boolean b) {
        putProperty(SHOW_INLINE, b);
    }

    @Override
    public boolean isShowInline() {
        return getBooleanProperty(SHOW_INLINE, DEFAULT_SHOW_INLINE);
    }

    @Override
    public void setShowTooltips(boolean b) {
        putProperty(SHOW_TOOLTIPS, b);
    }

    @Override
    public boolean isShowTooltips() {
        return getBooleanProperty(SHOW_TOOLTIPS, DEFAULT_SHOW_TOOLTIPS);
    }

    @Override
    public void setAutoRefresh(boolean b) {
        putProperty(AUTO_REFRESH, b);
    }

    @Override
    public boolean isAutoRefresh() {
        return getBooleanProperty(AUTO_REFRESH, DEFAULT_AUTO_REFRESH);
    }

    @Override
    public void setPeriodicRefresh(boolean b) {
        putProperty(PERIODIC_REFRESH, b);
    }

    @Override
    public boolean isPeriodicRefresh() {
        return getBooleanProperty(PERIODIC_REFRESH, DEFAULT_PERIODIC_REFRESH);
    }

    @Override
    public void setAutoRefreshInterval(int i) {
        putProperty(AUTO_REFRESH_INTERVAL, i);
    }

    @Override
    public int getAutoRefreshInterval() {
        return getIntProperty(AUTO_REFRESH_INTERVAL, DEFAULT_AUTO_REFRESH_INTERVAL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Regexp> getRegexpContexts() {
        return (List<Regexp>) getProperty(REGEXP_CONTEXTS, DEFAULT_REGEXP_CONTEXTS);
    }

    @Override
    public void setRegexpContexts(List<Regexp> l) {
        putProperty(REGEXP_CONTEXTS, l);
    }

    @Override
    public void setSpan(String interval) {
        putProperty(SPAN, interval);
    }

    @Override
    public String getSpan() {
        return (String) getProperty(SPAN, DEFAULT_SPAN);
    }

    @Override
    public Color getCoveredStripe() {
        return getColorProperty(COVERED_STRIPE);
    }

    @Override
    public void setCoveredStripe(Color c) {
        putColorProperty(COVERED_STRIPE, c);
    }

    @Override
    public Color getFailedCoveredStripe() {
        return getColorProperty(FAILED_COVERED_STRIPE);
    }

    @Override
    public void setFailedCoveredStripe(Color c) {
        putColorProperty(FAILED_COVERED_STRIPE, c);
    }

    @Override
    public Color getCoveredHighlight() {
        return getColorProperty(COVERED_HIGHLIGHT);
    }

    @Override
    public void setCoveredHighlight(Color c) {
        putColorProperty(COVERED_HIGHLIGHT, c);
    }

    @Override
    public Color getFailedCoveredHighlight() {
        return getColorProperty(FAILED_COVERED_HIGHLIGHT);
    }

    @Override
    public void setFailedCoveredHighlight(Color c) {
        putColorProperty(FAILED_COVERED_HIGHLIGHT, c);
    }

    @Override
    public Color getNotCoveredStripe() {
        return getColorProperty(NOT_COVERED_STRIPE);
    }

    @Override
    public void setNotCoveredStripe(Color c) {
        putColorProperty(NOT_COVERED_STRIPE, c);
    }

    @Override
    public Color getNotCoveredHighlight() {
        return getColorProperty(NOT_COVERED_HIGHLIGHT);
    }

    @Override
    public void setNotCoveredHighlight(Color c) {
        putColorProperty(NOT_COVERED_HIGHLIGHT, c);
    }

    @Override
    public Color getOutOfDateStripe() {
        return getColorProperty(OUTOFDATE_STRIPE);
    }

    @Override
    public void setOutOfDateStripe(Color c) {
        putColorProperty(OUTOFDATE_STRIPE, c);
    }

    @Override
    public Color getOutOfDateHighlight() {
        return getColorProperty(OUTOFDATE_HIGHLIGHT);
    }

    @Override
    public void setOutOfDateHighlight(Color c) {
        putColorProperty(OUTOFDATE_HIGHLIGHT, c);
    }

    @Override
    public Color getFilteredStripe() {
        return getColorProperty(FILTERED_STRIPE);
    }

    @Override
    public void setFilteredStripe(Color c) {
        putColorProperty(FILTERED_STRIPE, c);
    }

    @Override
    public Color getFilteredHighlight() {
        return getColorProperty(FILTERED_HIGHLIGHT);
    }

    @Override
    public void setFilteredHighlight(Color c) {
        putColorProperty(FILTERED_HIGHLIGHT, c);
    }

    @Override
    public String getLanguageLevel() {
        return (String) getProperty(LANGUAGE_LEVEL, DEFAULT_LANGUAGE_LEVEL);
    }

    @Override
    public void setLanguageLevel(String str) {
        putProperty(LANGUAGE_LEVEL, str);
    }

    @Override
    public boolean isLoadPerTestData() {
        return getBooleanProperty(LOAD_PER_TEST_DATA, DEFAULT_LOAD_PER_TEST_DATA);
    }

    @Override
    public void setLoadPerTestData(boolean loadPerTestData) {
        putProperty(LOAD_PER_TEST_DATA, loadPerTestData);
    }

    @Override
    public ProjectRebuild getProjectRebuild() {
        return (ProjectRebuild) getProperty(PROJECT_REBUILD, DEFAULT_PROJECT_REBUILD);
    }

    @Override
    public void setProjectRebuild(ProjectRebuild projectRebuild) {
        putProperty(PROJECT_REBUILD, projectRebuild);
    }

    protected void setDefaults(Map<String, Object> propertyMap) {
        propertyMap.put(ENABLED, DEFAULT_ENABLED);
        propertyMap.put(FLUSH_POLICY, DEFAULT_FLUSH_POLICY);
        propertyMap.put(FLUSH_INTERVAL, DEFAULT_FLUSH_INTERVAL);
        propertyMap.put(AUTO_REFRESH, DEFAULT_AUTO_REFRESH);
        propertyMap.put(PERIODIC_REFRESH, DEFAULT_PERIODIC_REFRESH);
        propertyMap.put(AUTO_REFRESH_INTERVAL, DEFAULT_AUTO_REFRESH_INTERVAL);
        propertyMap.put(SHOW_GUTTER, DEFAULT_SHOW_GUTTER);
        propertyMap.put(SHOW_INLINE, DEFAULT_SHOW_INLINE);
        propertyMap.put(SHOW_TOOLTIPS, DEFAULT_SHOW_TOOLTIPS);
        propertyMap.put(LANGUAGE_LEVEL, DEFAULT_LANGUAGE_LEVEL);

        propertyMap.put(AUTO_INIT_STRING, DEFAULT_AUTO_INIT_STRING);

        propertyMap.put(
                COVERED_HIGHLIGHT,
                Pair.of(DEFAULT_LIGHT_COVERED_HIGHLIGHT, DEFAULT_DARCULA_COVERED_HIGHLIGHT));
        propertyMap.put(
                COVERED_STRIPE,
                Pair.of(DEFAULT_LIGHT_COVERED_STRIPE, DEFAULT_DARCULA_COVERED_STRIPE));
        propertyMap.put(
                FAILED_COVERED_HIGHLIGHT,
                Pair.of(DEFAULT_LIGHT_FAILED_COVERED_HIGHLIGHT, DEFAULT_DARCULA_FAILED_COVERED_HIGHLIGHT));
        propertyMap.put(
                FAILED_COVERED_STRIPE,
                Pair.of(DEFAULT_LIGHT_FAILED_COVERED_STRIPE, DEFAULT_DARCULA_FAILED_COVERED_STRIPE));
        propertyMap.put(
                NOT_COVERED_HIGHLIGHT,
                Pair.of(DEFAULT_LIGHT_NOT_COVERED_HIGHLIGHT, DEFAULT_DARCULA_NOT_COVERED_HIGHLIGHT));
        propertyMap.put(
                NOT_COVERED_STRIPE,
                Pair.of(DEFAULT_LIGHT_NOT_COVERED_STRIPE, DEFAULT_DARCULA_NOT_COVERED_STRIPE));
        propertyMap.put(
                OUTOFDATE_HIGHLIGHT,
                Pair.of(DEFAULT_LIGHT_OUTOFDATE_HIGHLIGHT, DEFAULT_DARCULA_OUTOFDATE_HIGHLIGHT));
        propertyMap.put(
                OUTOFDATE_STRIPE,
                Pair.of(DEFAULT_LIGHT_OUTOFDATE_STRIPE, DEFAULT_DARCULA_OUTOFDATE_STRIPE));
        propertyMap.put(
                FILTERED_HIGHLIGHT,
                Pair.of(DEFAULT_LIGHT_FILTERED_HIGHLIGHT, DEFAULT_DARCULA_FILTERED_HIGHLIGHT));
        propertyMap.put(
                FILTERED_STRIPE,
                Pair.of(DEFAULT_LIGHT_FILTERED_STRIPE, DEFAULT_DARCULA_FILTERED_STRIPE));

        propertyMap.put(CONTEXT, DEFAULT_CONTEXT);
        propertyMap.put(REGEXP_CONTEXTS, DEFAULT_REGEXP_CONTEXTS);
        propertyMap.put(PROJECT_REBUILD, DEFAULT_PROJECT_REBUILD);

        propertyMap.put(BUILD_WITH_CLOVER, DEFAULT_BUILD_WITH_CLOVER);
    }

    @Override
    public boolean isBuildWithClover() {
        return getBooleanProperty(BUILD_WITH_CLOVER, DEFAULT_BUILD_WITH_CLOVER);
    }

    @Override
    public void setBuildWithClover(boolean buildWithClover) {
        putProperty(BUILD_WITH_CLOVER, buildWithClover);
    }

    @Override
    public void setInstrumentTests(boolean b) {
        putProperty(INSTRUMENT_TESTS, b);
    }

    @Override
    public boolean isInstrumentTests() {
        return getBooleanProperty(INSTRUMENT_TESTS, DEFAULT_INSTRUMENT_TESTS);
    }

    @Override
    public void setDumpInstrumentedSources(boolean dump) {
        putProperty(DUMP_INSTRUMENTED_SOURCES, dump);
    }

    @Override
    public boolean isDumpInstrumentedSources() {
        return getBooleanProperty(DUMP_INSTRUMENTED_SOURCES, DEFAULT_DUMP_INSTRUMENTED_SOURCES);
    }

    @Override
    public void setInstrumentLambda(LambdaInstrumentation instrumentLambda) {
        putProperty(INSTRUMENT_LAMBDA, instrumentLambda);
    }

    @Override
    public LambdaInstrumentation getInstrumentLambda() {
        return (LambdaInstrumentation )getProperty(INSTRUMENT_LAMBDA, DEFAULT_INSTRUMENT_LAMBDA);
    }
}
