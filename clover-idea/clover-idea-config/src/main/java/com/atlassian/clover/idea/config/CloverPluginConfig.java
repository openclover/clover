package com.atlassian.clover.idea.config;

import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import com.atlassian.clover.idea.config.regexp.Regexp;

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

    String LANGUAGE_LEVEL_JAVA_13 = "1.3";
    String LANGUAGE_LEVEL_JAVA_14 = "1.4 - 'assert' keyword";
    String LANGUAGE_LEVEL_JAVA_15 = "1.5 - 'enum' keyword, autoboxing, etc.";
    String LANGUAGE_LEVEL_JAVA_17 = "1.7 - try resource blocks, multi-catch clauses, diamond generics, binary literals.";
    String LANGUAGE_LEVEL_JAVA_18 = "1.8 - lambda functions, virtual extension methods.";
    String LANGUAGE_LEVEL_JAVA_19 = "9 - modules.";

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

    String getLanguageLevelAsNumber();

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