package org.openclover.idea.coverage.monitor;

import com.intellij.openapi.application.ApplicationManager;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.config.MappedCloverPluginConfig;
import org.openclover.idea.feature.FeatureEvent;
import org.openclover.idea.feature.FeatureListener;
import org.openclover.runtime.Logger;

import java.beans.PropertyChangeEvent;

/**
 * Monitor the CoverageModel for new coverage data.
 */
public class ThreadedDatabaseMonitor extends AbstractCoverageMonitor implements FeatureListener, ConfigChangeListener {

    private final Logger LOG = Logger.getInstance(ThreadedDatabaseMonitor.class.getName());

    private final Object LOCK = new Object();
    private boolean running = false;
    private boolean cleanup = false;

    private long interval = 2000;

    public ThreadedDatabaseMonitor() {
        final Thread thread = new Thread(() -> {
            while (!cleanup) {
                synchronized (LOCK) {
                    while (!running && !cleanup) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException e) {
                            LOG.debug("ThreadedDatabaseMonitor LOCK.wait was interrupted");
                        }
                    }
                }
                if (cleanup) {
                    break;
                }
                if (running) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        LOG.verbose("ThreadedDatabaseMonitor checking canLoadCoverageData.");
                        if (coverageManager.canLoadCoverageData()) {
                            LOG.verbose("-> Can load");
                            coverageManager.loadCoverageData(false);
                        }
                    });
                    try {
                        Thread.sleep(interval);
                    } catch (Exception e) {
                        LOG.debug("ThreadedDatabaseMonitor thread sleep was interrupted");
                    }
                }
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public void setMonitorInterval(long l) {
        this.interval = l;
    }

    @Override
    public void start() {
        LOG.verbose("start()");
        if (coverageManager == null) {
            throw new IllegalStateException("Can not start start threaded " +
                    "database monitor without first providing a coverage " +
                    "manager to monitor");
        }
        synchronized (LOCK) {
            running = true;
            LOCK.notify();
        }
    }

    @Override
    public void stop() {
        LOG.verbose("stop()");
        running = false;
    }

    public void cleanup() {
        LOG.verbose("cleanup()");
        cleanup = true;
        if (!running) {
            synchronized (LOCK) {
                LOCK.notify();
            }
        }
    }

    @Override
    public void featureStateChanged(FeatureEvent evt) {
        if (evt.isEnabled()) {
            start();
        } else {
            stop();
        }
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(MappedCloverPluginConfig.AUTO_REFRESH_INTERVAL)) {
            PropertyChangeEvent changeEvent = evt.getPropertyChange(MappedCloverPluginConfig.AUTO_REFRESH_INTERVAL);
            setMonitorInterval(((Integer) changeEvent.getNewValue()).longValue());
        }
    }
}
