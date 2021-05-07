package org.openclover.eclipse.core;

import com.atlassian.clover.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;

/**
 * Adapts the Eclipse platform logging to a Clover logger
 */
class PluginLoggingAdapter extends Logger {

    private ILog log;
    private boolean enabled;

    PluginLoggingAdapter(ILog log, boolean enabled) {
        super();
        this.log = log;
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void log(int level, String msg, Throwable t) {
        if (enabled && !canIgnore(level)) {
            int eclipseLevel;
            switch (level) {
                case Logger.LOG_DEBUG:
                case Logger.LOG_VERBOSE:
                    eclipseLevel = IStatus.OK;
                    break;
                case Logger.LOG_INFO:
                    eclipseLevel = IStatus.INFO;
                    break;
                case Logger.LOG_WARN:
                    eclipseLevel = IStatus.WARNING;
                    break;
                case Logger.LOG_ERR:
                    eclipseLevel = IStatus.ERROR;
                    break;
                default:
                    eclipseLevel = IStatus.OK;
            }
            log.log(new Status(eclipseLevel, CloverPlugin.ID, 0, msg, t));
        }
    }
}
