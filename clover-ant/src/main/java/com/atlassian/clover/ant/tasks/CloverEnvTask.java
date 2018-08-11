package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.PrematureLibraryLoader;
import com_atlassian_clover.CloverVersionInfo;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Diagnostics;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.openclover.util.ClassPathUtil;

import java.net.URL;


public class CloverEnvTask extends Task {

    @Override
    public void init() throws BuildException {
        PrematureLibraryLoader.doOnce();
        super.init();
    }

    /**
     * Execute should never be called, in case it does, this
     * task will merely log the Clover version being used, and some environment
     * information.
     */
    @Override
    public void execute() throws BuildException {
        logEnvironment();

        ProjectHelper helper = (ProjectHelper) getProject().getReference(ProjectHelper.PROJECTHELPER_REFERENCE);
        try {
            URL importURL = CloverEnvTask.class.getResource("clover.xml");
            log("Loading clover.xml: " + importURL);
            helper.parse(getProject(), importURL);
        } catch (BuildException ex) {
            throw ProjectHelper.addLocationToBuildException(ex, getLocation());
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
