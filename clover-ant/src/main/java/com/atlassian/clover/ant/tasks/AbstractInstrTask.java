package com.atlassian.clover.ant.tasks;

import clover.org.apache.commons.lang3.StringUtils;
import com.atlassian.clover.Logger;
import com.atlassian.clover.ant.AntCloverProfile;
import com.atlassian.clover.ant.AntCloverProfiles;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.cfg.instr.InstrumentationLevel;
import com.atlassian.clover.cfg.instr.MethodContextDef;
import com.atlassian.clover.cfg.instr.StatementContextDef;
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import com.atlassian.clover.cfg.instr.java.SourceLevel;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.remote.DistributedConfig;
import com_atlassian_clover.CloverProfile;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;


public abstract class AbstractInstrTask extends AbstractCloverTask {

    protected boolean optimizationOnly = false;

    public void setReportInitErrors(boolean reportInitErrors) {
        config.setReportInitErrors(reportInitErrors);
    }

    /**
     * set the default source level to process files at
     *
     * @param source the src level - e.g. "8", "11", "17"
     */
    public void setSource(String source) {
        if (SourceLevel.isUnsupported(source)) {
            Logger.getInstance().warn(SourceLevel.getUnsupportedMessage(source));
        }
        config.setSourceLevel(SourceLevel.fromString(source));
    }

    public void setFullyQualifyJavaLang(boolean fullyQualify) {
        config.setFullyQualifyJavaLang(fullyQualify);
    }

    public void setRecordTestResults(boolean record) {
        config.setRecordTestResults(record);
    }

    public void addConfiguredDistributedCoverage(DistributedConfig distConfig) {
        config.setDistributedConfig(distConfig);
    }

    /**
     * Setter for Ant's:
     * <pre>
     *     &lt;clover-setup|clover-instr&gt;
     *         &lt;profiles&gt; ... &lt;/profiles&gt;
     *     &lt;/clover-setup|clover-instr&gt;
     * </pre>
     */
    public void addConfiguredProfiles(AntCloverProfiles profiles) {
        // validate - we must have at least one profile named "default"
        boolean found = false;
        for (AntCloverProfile p : profiles.getProfiles()) {
            if (p.getName().equals(CloverProfile.DEFAULT_NAME)) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("You have defined <profiles> but there is no default <profile> defined. "
                    + "You must have one <profile name=\"default\" .../> declared.");
        }

        // copy profiles to instrumentation config
        for (AntCloverProfile p : profiles.getProfiles()) {
            config.addProfile(p);
        }
    }

    public String getRuntimeInitString() {

        try {
            return config.getRuntimeInitString();
        } catch (CloverException e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    public void setEncoding(String encoding) {
        config.setEncoding(encoding);
    }

    /**
     * Flag to indicate whether the initstring should be treated as a relative path
     *
     * @param relative if true, the initstring is treated as a relative path
     */
    public void setRelative(boolean relative) {
        config.setRelative(relative);
    }

    /**
     * Controls the flush intervalu for interval-based flushing.
     *
     * @param flushInterval flush interval in milliseconds.
     */
    public void setFlushInterval(int flushInterval) {
        config.setFlushInterval(flushInterval);
    }

    /**
     * Set the flush policy which controls when Clover will flush coverage data to the clover database.
     *
     * @param flushPolicy the flush policy to use.
     */
    public void setFlushPolicy(AntInstrumentationConfig.FlushPolicy flushPolicy) {
        config.setFlushPolicy(flushPolicy.getIndex());
    }

    /**
     * Set the instrumentation strategy
     *
     * @param instrumentation strategy to use.
     */
    public void setInstrumentation(AntInstrumentationConfig.Instrumentation instrumentation) {
        config.setInstrStrategy(instrumentation.getValue());
    }

    public void setInstrumentationLevel(AntInstrumentationConfig.EnumInstrumentationLevel instrumentationLevel) {
        config.setInstrLevel(instrumentationLevel.getIndex());
    }

    /**
     * Set how lambda expressions (Java8) shall be instrumented.
     *
     * @param level one of: "none, expression, block, all"
     */
    public void setInstrumentLambda(String level) {
        try {
            config.setInstrumentLambda(level);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("You must use one of the following values for lambda instrumentation: ["
                    + StringUtils.join(LambdaInstrumentation.values(), ", ") + "] instead of [" + level + "]");
        }
    }

    /**
     * Optimization only implies method level instrumentation
     */
    public void setOptimizationOnly(boolean optimizationOnly) {
        this.optimizationOnly = optimizationOnly;
    }

    public void addConfiguredFileSet(FileSet set) {
        config.addConfiguredFileSet(set);
    }

    public void addConfiguredTestSources(TestSourceSet ts) {
        config.addConfiguredTestSources(ts);
    }

    public void addMethodContext(MethodContextDef context) {
        config.addMethodContext(context);
    }

    public void addStatementContext(StatementContextDef context) {
        config.addStatementContext(context);
    }

    @Override
    public boolean validate() {

        if (config.isIntervalBasedFlushing() && config.getFlushInterval() == 0) {
            throw new BuildException("You must set a flushinterval > 0 when using '" + config.getFlushPolicyString() + "' flushpolicy.");
        }

        if (optimizationOnly) {
            // optimization only implies method level only instrumentation
            final AntInstrumentationConfig.EnumInstrumentationLevel instrumentationLevel =
                    (AntInstrumentationConfig.EnumInstrumentationLevel)
                            AntInstrumentationConfig.EnumInstrumentationLevel.getInstance(
                                    AntInstrumentationConfig.EnumInstrumentationLevel.class,
                                    InstrumentationLevel.METHOD.name().toLowerCase());
            config.setInstrLevelStrategy(instrumentationLevel.getValue());
        }


        try {
            ContextStore.saveCustomContexts(config);
        } catch (CloverException e) {
            throw new BuildException(e.getMessage(), e);
        }

        return true;
    }
}
