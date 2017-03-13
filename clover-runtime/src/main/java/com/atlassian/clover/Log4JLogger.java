package com.atlassian.clover;

import clover.org.apache.log4j.Level;

import java.lang.reflect.Method;

public class Log4JLogger extends Logger {

    private clover.org.apache.log4j.Logger instance;

    private static Level[] LOG4J_LEVELS;

    public Log4JLogger(String category) {
        instance = clover.org.apache.log4j.Logger.getLogger(category);
    }

    @Override
    public void log(int level, String msg, Throwable t) {
        instance.log(LOG4J_LEVELS[level], msg, t);
    }

    public static boolean init() {
        Throwable t = null;
        try {
            findLogMethod();
            LOG4J_LEVELS =  new Level[] {
                    Level.ERROR,
                    Level.WARN,
                    Level.INFO,
                    Level.DEBUG,
                    Level.DEBUG
            };
           return true;
        } catch (ClassNotFoundException e) {
           Logger.getInstance().debug("Error initialising Log4J", e);
        } catch (LinkageError e) {
           Logger.getInstance().debug("Error initialising Log4J", e);
        } catch (NoSuchMethodException e) {
           Logger.getInstance().debug("Error initialising Log4J", e);
        }
        return false;
    }

    public static void findLogMethod() throws ClassNotFoundException, NoSuchMethodException {
        Class categoryClass = Class.forName("clover.org.apache.log4j.Category");
        Class priorityClass = Class.forName("clover.org.apache.log4j.Priority");
        Method logMethod = categoryClass.getDeclaredMethod("log",
                new Class[] {priorityClass, Object.class, Throwable.class});
    }

    public static class Factory implements Logger.Factory {

        @Override
        public Logger getLoggerInstance(String category) {
             return new Log4JLogger(category);
        }
    }

}
