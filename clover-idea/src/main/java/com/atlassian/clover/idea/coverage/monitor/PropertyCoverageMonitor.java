package com.atlassian.clover.idea.coverage.monitor;

import com.atlassian.clover.Logger;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.idea.config.ConfigChangeEvent;
import com.atlassian.clover.idea.config.ConfigChangeListener;
import com.atlassian.clover.idea.config.MappedCloverPluginConfig;

import java.net.URL;

/**
 * Monitor the Config object for property changes that impact upon the
 * coverage model.
 *
 * @see MappedCloverPluginConfig#SPAN
 * @see MappedCloverPluginConfig#CONTEXT
 * @see MappedCloverPluginConfig#REGEXP_CONTEXTS
 * @see MappedCloverPluginConfig#RELATIVE_INIT_STRING
 * @see MappedCloverPluginConfig#MANUAL_INIT_STRING
 * @see MappedCloverPluginConfig#AUTO_INIT_STRING
 */
public class PropertyCoverageMonitor extends AbstractCoverageMonitor implements ConfigChangeListener {

    private final Logger LOG = Logger.getInstance("PropertyCoverageMonitor");

    private final CloverPluginConfig config;

    public PropertyCoverageMonitor(CloverPluginConfig data) {
        config = data;
    }

    @Override
    public void start() {
        config.addConfigChangeListener(this);
        // updateManager(config);
    }

    @Override
    public void stop() {
        config.removeConfigChangeListener(this);
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(MappedCloverPluginConfig.SPAN) ||
                evt.hasPropertyChange(MappedCloverPluginConfig.CONTEXT) ||
                evt.hasPropertyChange(MappedCloverPluginConfig.REGEXP_CONTEXTS) ||
                evt.hasPropertyChange(MappedCloverPluginConfig.RELATIVE_INIT_STRING) ||
                evt.hasPropertyChange(MappedCloverPluginConfig.MANUAL_INIT_STRING) ||
                evt.hasPropertyChange(MappedCloverPluginConfig.AUTO_INIT_STRING)) {
            updateManager((CloverPluginConfig) evt.getSource());
        }
    }

    private void updateManager(CloverPluginConfig data) {
        try {
            String context = data.getContextFilterSpec();
            String initString = data.getInitString();
            URL initUrl = new URL("file", "localhost", initString);
            coverageManager.setInitString(initUrl);
            coverageManager.setContextFilter(context);
            coverageManager.setSpan(new Interval(data.getSpan()).getValueInMillis());
            coverageManager.reload();
        } catch (Exception e) {
            LOG.error(e);
        }
    }
}
