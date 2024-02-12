package org.openclover.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class Logger {
    //these values mirror Ant's values
    public static final int LOG_ERR = 0;
    public static final int LOG_WARN = 1;
    public static final int LOG_INFO = 2;
    public static final int LOG_VERBOSE = 3;
    public static final int LOG_DEBUG = 4;

    /**
     * For backward compatibility, support the overriding the factory
     * with a singleton instance of the logger.
     */
    private static final Logger NULL_LOGGER = new NullLogger();
    private static Logger SINGLETON = null;
    private static Factory FACTORY_INSTANCE = new Factory() {
        @Override
        public Logger getLoggerInstance(String category) {
            return new DefaultLogger();
        }
    };
    private static boolean debug = false;
    private static boolean verbose = false;

    //Ensures logging level is always set if clover.logging.level is set as a sysprop
    static {
        try {
            String level = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(CloverNames.PROP_LOGGING_LEVEL);
                }
            });
            if ("debug".equalsIgnoreCase(level)) {
                setDebug(true);
            } else if ("verbose".equalsIgnoreCase(level)) {
                setVerbose(true);
            }
        } catch (SecurityException e) {
            System.err.println("Security exception trying to initialise Clover logging: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public interface Factory {
        Logger getLoggerInstance(String category);
    }

    public static class NullLogger extends Logger {
        @Override
        public void log(int level, String msg, Throwable t) {
            //no-op
        }
    }

    public static Logger getInstance(String category) {
        if (SINGLETON != null) {
            return SINGLETON;
        }
        return FACTORY_INSTANCE == null ? NULL_LOGGER : FACTORY_INSTANCE.getLoggerInstance(category);
    }

    public static Logger getInstance() {
        return getInstance("org.openclover.eclipse.core_v" + org_openclover_runtime.CloverVersionInfo.RELEASE_NUM);
    }

    public static void setFactory(Factory factory) {
        FACTORY_INSTANCE = factory;
    }

    /**
     * If set, this instance will be returned by all requests made to getInstance,
     * overriding the factory implementation and thereby providing backward
     * compatibility.
     */
    public static void setInstance(Logger instance) {
        SINGLETON = instance;
    }

    public static void setDebug(boolean debug) {
        Logger.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static void setVerbose(boolean verbose) {
        Logger.verbose = verbose;
    }

    public static boolean canIgnore(int level) {
        if (!debug && (level == LOG_DEBUG)) {
            return true;
        }
        return !(verbose || debug) && (level == LOG_VERBOSE);
    }

    public void error(String msg) {
        log(LOG_ERR, msg, null);
    }

    public void error(String msg, Throwable t) {
        log(LOG_ERR, msg, t);
    }

    public void error(Throwable t) {
        log(LOG_ERR, (t != null) ? t.getMessage() : "Exception", t);
    }

    public void warn(String msg) {
        log(LOG_WARN, msg, null);
    }

    public void warn(String msg, Throwable t) {
        log(LOG_WARN, msg, t);
    }

    public void warn(Throwable t) {
        log(LOG_WARN, (t != null) ? t.getMessage() : "Exception", t);
    }

    public void info(String msg) {
        log(LOG_INFO, msg, null);
    }

    public void info(String msg, Throwable t) {
        log(LOG_INFO, msg, t);
    }

    public void info(Throwable t) {
        log(LOG_INFO, (t != null) ? t.getMessage() : "Exception", t);
    }

    public void verbose(String msg) {
        log(LOG_VERBOSE, msg, null);
    }

    public void verbose(String msg, Throwable t) {
        log(LOG_VERBOSE, msg, t);
    }

    public void verbose(Throwable t) {
        log(LOG_VERBOSE, (t != null) ? t.getMessage() : "Exception", t);
    }

    public void debug(String msg) {
        log(LOG_DEBUG, msg, null);
    }

    public void debug(String msg, Throwable t) {
        log(LOG_DEBUG, msg, t);
    }

    public void debug(Throwable t) {
        log(LOG_DEBUG, (t != null) ? t.getMessage() : "Exception", t);
    }

    protected Logger() {
    }

    public abstract void log(int level, String msg, Throwable t);
}

