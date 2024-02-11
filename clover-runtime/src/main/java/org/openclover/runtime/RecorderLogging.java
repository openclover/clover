package org.openclover.runtime;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This class manages setting up logging during recording
 */
public class RecorderLogging {
    public static void init() {
        try {
            String adapter = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(CloverNames.PROP_LOGGING_ADAPTER);
                }
            });
            if (adapter != null && adapter.length() > 0) {
                if ("slf4j".equalsIgnoreCase(adapter)) {
                    initSLF4JLogging();
                } else if ("jdk".equalsIgnoreCase(adapter)) {
                    initJDKLogging();
                } else if (!"stderr".equals(adapter)) {
                    initCustomLogging(adapter);
                }
            }
        } catch (SecurityException e) {
            // can't do much if no perms
            Logger.getInstance().info("Security exception trying to initialise Clover logging", e);
        }
    }

    private static void initSLF4JLogging() {
        if (SLF4JLogger.init()) {
            Logger.setFactory(new SLF4JLogger.Factory());
        } else {
            Logger.getInstance().error("Unable to initialise SLF4J Logger. Using default logger.");
        }
    }

    private static void initJDKLogging() {
        try {
            Class.forName("java.util.logging.Logger"); // bug out here if we aren't running jdk1.4
            Logger.setFactory(new JDKLogger.Factory());
        } catch (ClassNotFoundException e) {
            Logger.getInstance().error("Unable to initialise JDK Logger. Using default logger.");
        }
    }

    private static void initCustomLogging(String classname) {
        String errorMsg = "Unable to initialise Logger class '" + classname + "'. ";
        boolean successful = false;
        try {
            Class loggerFactoryClass = Class.forName(classname);
            if (Logger.Factory.class.isAssignableFrom(loggerFactoryClass)) {
                Logger.Factory factory = (Logger.Factory) loggerFactoryClass.getDeclaredConstructor().newInstance();
                Logger.setFactory(factory);
                successful = true;
            } else {
                Logger.getInstance().error(errorMsg + " The class must be a subclass of " +
                        Logger.Factory.class.getName() + ". Using default logger.");
            }

        } catch (ClassNotFoundException e) {
            errorMsg += "Class not found. ";
        } catch (ExceptionInInitializerError | InvocationTargetException e) {
            errorMsg += "An error occured during class initialisation. ";
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            errorMsg += "An error occured during class initialisation. The class must provide a no-args" +
                    " public constructor. ";
        }

        if (!successful) {
            errorMsg += " Using default logger";
            Logger.getInstance().error(errorMsg);
        }
    }
}