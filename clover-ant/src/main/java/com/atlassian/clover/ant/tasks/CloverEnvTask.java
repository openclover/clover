package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.PrematureLibraryLoader;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.util.ClassPathUtil;
import com_atlassian_clover.CloverVersionInfo;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Diagnostics;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This task must be loaded before all other
 * clover tasks. It ensures that clover is on
 * the System classpath, setting it if not.
 */
public class CloverEnvTask extends Task {
    /**
     * The Clover compiler adapter class
     */
    public static final String CLOVER_ADAPTER = "com.atlassian.clover.ant.taskdefs.CloverCompilerAdapter";

    static {
        // check that Clover can be loaded correctly and extend classpath if not
        try {
            final String cloverJarPath = ClassPathUtil.getCloverJarPath();

            if (cloverJarPath != null) {
                try {
                    URLClassLoader loader = ClassPathUtil.findSystemClassLoader(Project.class);
                    if (!ClassPathUtil.isClassOnClassPath(CLOVER_ADAPTER, loader)) {
                        ClassPathUtil.extendClassPath(cloverJarPath, loader);
                        logDebug("Added '" + cloverJarPath + "' to the System Classloader '" + loader + "'");
                        ClassPathUtil.assertOnClassPath(CLOVER_ADAPTER, loader);
                    } else {
                        logDebug(cloverJarPath + " is already on the System Classloader.");
                    }

                } catch (Throwable e) {
                    logDebug("Error adding Clover '" + cloverJarPath +
                            "' to the System Classloader: Cause " + e.getMessage(), e);
                }
            } else {
                logDebug("Unable to determine the Clover jar path. Not extending System Classloader.");
            }
        } catch (Throwable t) {
            logClassPathMessage();
            logDebug(t.getMessage(), t);
        }
    }

    private static void logClassPathMessage() {
        System.out.println(
                "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n" +
                "Error determining the correct path to clover.jar. \n" +
                "Ensure there is not an older copy of clover.jar on Ant's classpath. \n" +
                "NB: Clover no longer requires clover.jar to be placed in Ant's lib directory.\n" +
                "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    private static void logDebug(String message) {
        logDebug(message, null);
    }

    private static void logDebug(String message, Throwable t) {
        if (isDebug()) {
            System.out.println(message);
            if (t != null) {
                t.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void init() throws BuildException {
        PrematureLibraryLoader.doOnce();
        super.init();
    }

    /**
     * Execute should never be called, in case it does, this
     * task will merely log the Clover version being used, and some environment
     * information.
     * @throws BuildException
     */
    @Override
    public void execute() throws BuildException {
        logEnvironment();

        ProjectHelper helper = (ProjectHelper) getProject().getReference(ProjectHelper.PROJECTHELPER_REFERENCE);
        try {
            URLClassLoader loader = ClassPathUtil.findSystemClassLoader(Project.class);

            URL importURL = loader.getResource("clover.xml");
            log("Loading clover.xml: " + importURL);
            helper.parse(getProject(), importURL);
        } catch (BuildException ex) {
            throw ProjectHelper.addLocationToBuildException(ex, getLocation());
        } catch (CloverException e) {
            log("Error importing clover.xml into project.");
            throw new BuildException(e.getMessage(), e);
        } catch (Throwable e) {
            log("An unexpected error occurred while trying to import the bundled clover build file.");
            throw new BuildException(e.getMessage(), e);
        }
    }

    private void logEnvironment() {
        if (isDebug()) {
            Diagnostics.doReport(System.out);
            log("Clover loaded from: " + ClassPathUtil.getCloverJarPath());
            log(CloverVersionInfo.formatVersionInfo());
        }
    }

    private static boolean isDebug() {
        //NB: when run from ANT, use ANT_OPTS system var since -D's are Ant project Properties!        
        return System.getProperty("clover.debug") != null;
    }
}
