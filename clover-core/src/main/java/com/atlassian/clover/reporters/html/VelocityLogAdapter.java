package com.atlassian.clover.reporters.html;

import clover.org.apache.velocity.runtime.RuntimeServices;
import clover.org.apache.velocity.runtime.log.LogChute;
import org.openclover.runtime.Logger;


public class VelocityLogAdapter implements LogChute {
    private Logger mLogger;

    public VelocityLogAdapter(Logger aLogger) {
        mLogger = aLogger;
    }

    /**
     * This init() will be invoked once by the LogManager
     * to give you current RuntimeServices intance
     */
    @Override
    public void init(RuntimeServices rsvc) {
        // do nothing
    }

    /**
     * This is the method that you implement for Velocity to call
     * with log messages.
     */
    @Override
    public void log(int level, String message, Throwable t) {

        final int logLevel;
        switch (level) {
            case LogChute.DEBUG_ID:
                logLevel = Logger.LOG_DEBUG;
                break;
            case LogChute.INFO_ID:
                logLevel = Logger.LOG_DEBUG; // Due to VelocimacroFactory:360 logging too much information
                break;
            case LogChute.WARN_ID:
                logLevel = Logger.LOG_WARN;
                break;
            case LogChute.ERROR_ID:
                logLevel = Logger.LOG_ERR;
                break;
            case LogChute.TRACE_ID:
                logLevel = Logger.LOG_VERBOSE;
                break;
            default:
                logLevel = Logger.LOG_DEBUG;
        }
        mLogger.log(logLevel, "[Velocity] " + message, t);
    }

    @Override
    public void log(int level, String message) {
        log(level, message, null);
    }

    @Override
    public boolean isLevelEnabled(int level) {
        if (level == LogChute.DEBUG_ID) {
            return Logger.isDebug() || Logger.isVerbose();
        }
        return true;
    }
}
