package com.atlassian.clover;

import java.util.logging.Level;

public class JDKLogger extends Logger {

    private java.util.logging.Logger instance;

    private static java.util.logging.Level[] LOG_LEVELS = new Level[] {
                    Level.SEVERE,
                    Level.WARNING,
                    Level.INFO,
                    Level.FINE,
                    Level.FINER  };

    public JDKLogger(String category) {
        instance = java.util.logging.Logger.getLogger(category);
    }

    @Override
    public void log(int level, String msg, Throwable t) {
        instance.log(LOG_LEVELS[level], msg, t);
    }

    public static class Factory implements Logger.Factory {

        @Override
        public Logger getLoggerInstance(String category) {
             return new JDKLogger(category);
        }
    }

}
