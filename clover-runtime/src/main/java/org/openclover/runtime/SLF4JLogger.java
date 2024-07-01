package org.openclover.runtime;


public class SLF4JLogger extends Logger {

    private final org.slf4j.Logger instance;

    public SLF4JLogger(String category) {
        instance = org.slf4j.LoggerFactory.getLogger(category);
    }

    @Override
    public void log(int level, String msg, Throwable t) {
        if (level == LOG_DEBUG || level == LOG_VERBOSE) {
            instance.debug(msg, t);
        } else if (level == LOG_INFO) {
            instance.info(msg, t);
        } else if (level == LOG_WARN) {
            instance.warn(msg, t);
        } else if (level == LOG_ERR) {
            instance.error(msg, t);
        }
    }

    public static boolean init() {
        try {
            Class.forName("clover.org.slf4j.Logger");
            Class.forName("clover.org.slf4j.LoggerFactory");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            Logger.getInstance().debug("Error initialising SLF4J", e);
            return false;
        }
    }

    public static class Factory implements Logger.Factory {

        @Override
        public Logger getLoggerInstance(String category) {
             return new SLF4JLogger(category);
        }
    }

}
