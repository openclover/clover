package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.optimization.Snapshot;
import org.openclover.runtime.Logger;
import org.apache.tools.ant.BuildException;

import java.io.File;

public class CloverCanOptimizeTests extends AbstractCloverTask implements org.apache.tools.ant.taskdefs.condition.Condition {
    private int fullRunEvery = 10;
    private String property;
    private String value = "true";
    private File snapshotFile;

    public void setProperty(String property) {
        this.property = property;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setFullRunEvery(int fullRunEvery) {
        this.fullRunEvery = fullRunEvery;
    }

    public int getFullRunEvery() {
        return fullRunEvery;
    }

    public void setSnapshotFile(File snapshotFile) {
        this.snapshotFile = snapshotFile;
    }

    @Override
    public boolean eval() throws BuildException {
        init();

        boolean result = false;
        Snapshot snapshot =
            snapshotFile == null
                ? Snapshot.loadFor(config.resolveInitString())
                : Snapshot.loadFromFile(snapshotFile);
        
        if (snapshot == null) {
            Logger.getInstance().info(
                "Clover can't optimize test runs for this build because the snapshot file " +
                    (snapshotFile == null
                        ? Snapshot.fileNameForInitString(config.resolveInitString())
                        : snapshotFile.getAbsolutePath())
                    + " was not found.");
        } else {
            StringBuffer reason = new StringBuffer();
            result = !snapshot.isTooStale(fullRunEvery, reason);
            Logger.getInstance().info(
                result
                    ? "Clover can optimize test runs for this build."
                    : reason.toString());
        }
        return result;
    }

    @Override
    public boolean validate() {
        if (property == null) {
            throw new BuildException("property is required");
        } else {
            return true;
        }
    }

    @Override
    public void cloverExecute() {
        if (eval() && property != null) {
            getProject().setProperty(property, value);
        }
    }
}
