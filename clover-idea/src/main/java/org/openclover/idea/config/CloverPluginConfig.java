package org.openclover.idea.config;

import org.openclover.core.cfg.instr.java.LambdaInstrumentation;
import org.openclover.idea.config.regexp.Regexp;

import java.awt.Color;
import java.util.List;

public interface CloverPluginConfig {

    //TODO: Need to split this interface into each of its components (- one per panel..?)
    //TODO: - init string
    //TODO: - flush policy
    //TODO: - refresh policy
    //TODO: - filter
    //TODO: - context
    //TODO: - highlight
    //TODO: Why? the CloverPluginConfig is becoming alittle too heavy with many
    //TODO: different configuration parameters, some of which are not always
    //TODO: required.

    int DIRECTED_FLUSHING = 0;
    int INTERVAL_FLUSHING = 1;
    int THREADED_FLUSHING = 2;

    String LANGUAGE_LEVEL_JAVA_9 = "9";
    String DEFAULT_LANGUAGE_LEVEL = LANGUAGE_LEVEL_JAVA_9;

    /**
     * Add a config change listener
     *
     * @param l ConfigChangeListener
     */
    void addConfigChangeListener(ConfigChangeListener l);

    /**
     * Remove a config change listener.
     *
     * @param l ConfigChangeListener
     */
    void removeConfigChangeListener(ConfigChangeListener l);

    /**
     * Trigger a notification of all registered config change listeners if
     * any properties have changed.
     */
    void notifyListeners();

    void putProperty(String name, Object value);

    Object getProperty(String name);

    String getInitString();

    String getGeneratedInitString();

    void setUseGeneratedInitString(boolean b);

    boolean getUseGeneratedInitString();

    void setManualInitString(String str);

    String getManualInitString();

    void setRelativeInitString(boolean b);

    boolean isRelativeInitString();

    void setEnabled(boolean b);

    boolean isEnabled();

    void setFlushPolicy(int flushPolicy);

    int getFlushPolicy();

    void setFlushInterval(int interval);

    int getFlushInterval();

    void setIncludes(String includes);

    String getIncludes();

    void setExcludes(String excludes);

    String getExcludes();

    void setContextFilterSpec(String ctx);

    String getContextFilterSpec();

    void setShowGutter(boolean b);

    boolean isShowGutter();

    void setShowInline(boolean b);

    boolean isShowInline();

    void setShowTooltips(boolean b);

    boolean isShowTooltips();

    void setAutoRefresh(boolean b);

    boolean isAutoRefresh();

    void setPeriodicRefresh(boolean b);

    boolean isPeriodicRefresh();

    void setAutoRefreshInterval(int l);

    int getAutoRefreshInterval();

    void setRegexpContexts(List<Regexp> l);

    List<Regexp> getRegexpContexts();

    void setSpan(String interval);

    String getSpan();

    Color getCoveredStripe();

    void setCoveredStripe(Color c);

    Color getCoveredHighlight();

    void setCoveredHighlight(Color c);

    Color getNotCoveredStripe();

    void setNotCoveredStripe(Color c);

    Color getNotCoveredHighlight();

    void setNotCoveredHighlight(Color c);

    Color getFailedCoveredStripe();

    void setFailedCoveredStripe(Color c);

    Color getFailedCoveredHighlight();

    void setFailedCoveredHighlight(Color c);

    Color getOutOfDateStripe();

    void setOutOfDateStripe(Color c);

    Color getOutOfDateHighlight();

    void setOutOfDateHighlight(Color c);

    Color getFilteredStripe();

    void setFilteredStripe(Color c);

    Color getFilteredHighlight();

    void setFilteredHighlight(Color c);

    String getLanguageLevel();

    void setLanguageLevel(String str);

    ProjectRebuild getProjectRebuild();

    void setProjectRebuild(ProjectRebuild projectRebuild);

    boolean isLoadPerTestData();

    void setLoadPerTestData(boolean loadPerTestData);

    boolean isBuildWithClover();

    void setBuildWithClover(boolean buildWithClover);

    public void setInstrumentTests(boolean b);

    public boolean isInstrumentTests();

    void setDumpInstrumentedSources(boolean dump);

    public boolean isDumpInstrumentedSources();

    LambdaInstrumentation getInstrumentLambda();

    void setInstrumentLambda(LambdaInstrumentation instrumentLambda);
}