package org.openclover.ant.tasks;

import org.openclover.core.util.CloverUtils;
import org.apache.tools.ant.BuildException;

public class CloverCleanTask extends AbstractCloverTask {

    private boolean keepDB = false;
    private boolean keepSnapshot = true;
    private boolean verbose = false;
    private boolean haltOnError = false;

    /**
     * If true, the coverage database itself will not be deleted.
     */
    public void setKeepdb(boolean b) {
        keepDB = b;
    }

    /**
     * If true, a statement will be logged for every file deleted.
     */
    public void setVerbose(boolean b) {
        verbose = b;
    }

    /**
     * If true, the test snapshot will not be deleted.
     */
    public void setKeepSnapshot(boolean b) {
        keepSnapshot = b;
    }

    /**
     * If true, a BuildException will be generated if a file can not be deleted.
     */
    public void setHaltOnError(boolean b) {
        haltOnError = b;
    }

    @Override
    public void cloverExecute() {
        if (!CloverUtils.scrubCoverageData(resolveInitString(), !keepDB, !keepSnapshot, verbose) && haltOnError) {
            throw new BuildException("Encountered problem deleting database. Check log for details.");
        }
    }
}
