package org.openclover.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.cfg.Interval;
import org.openclover.core.optimization.Snapshot;
import org.openclover.core.optimization.SnapshotPrinter;
import org.openclover.core.recorder.PerTestCoverageStrategy;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;

import java.io.File;
import java.util.LinkedList;

import static org.openclover.core.util.Lists.newLinkedList;

public class CloverSnapshotTask extends AbstractCloverTask {
    private Interval initialSpan = Interval.DEFAULT_SPAN;
    private File file;

    public void setSpan(String initialspan) {
        this.initialSpan = new Interval(initialspan);
    }
    
    public void setInitialSpan(String initialspan) {
        this.initialSpan = new Interval(initialspan);
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public void cloverExecute() {
        Snapshot.setDebug(debug);

        final File snapshotLocation = file != null ? file : Snapshot.fileForInitString(config.resolveInitString());
        try {
            Logger.getInstance().verbose("Attempting to load snapshot file from '" + snapshotLocation + "'");

            Snapshot snapshot = Snapshot.loadFromFile(snapshotLocation);

            long start;
            final PerTestCoverageStrategy perTestCoverageStrategy = getPerTestCoverageStrategy();
            Logger.getInstance().verbose("PerTestCoverageStrategy is: " + perTestCoverageStrategy);
            if (snapshot == null) {
                Logger.getInstance().info("Snapshot file not found, creating new file at " + snapshotLocation.getAbsolutePath());

                start = System.currentTimeMillis();
                CloverDatabase db =
                    CloverDatabase.loadWithCoverage(
                        config.resolveInitString(),
                        new CoverageDataSpec(null, initialSpan.getValueInMillis(), false, true, true, true, perTestCoverageStrategy));
                Logger.getInstance().verbose("Took " + (System.currentTimeMillis() - start) + "ms to load coverage data to generate the snapshot");

                snapshot = Snapshot.generateFor(db, snapshotLocation.getAbsolutePath());
            } else {
                if (debug) {
                    SnapshotPrinter.textPrint(snapshot, Logger.getInstance(), Logger.LOG_INFO);
                }

                final LinkedList<Long> versions = newLinkedList(snapshot.getDbVersions());
                final long lastVersion = versions.size() == 0 ? initialSpan.getValueInMillis() : versions.getLast();
                final long span = Math.max(0, System.currentTimeMillis() - lastVersion); 
                start = System.currentTimeMillis();
                CloverDatabase db =
                    CloverDatabase.loadWithCoverage(
                        config.resolveInitString(),
                        new CoverageDataSpec(null, span, false, true, true, true, perTestCoverageStrategy));
                Logger.getInstance().verbose("Took " + (System.currentTimeMillis() - start) + "ms to load coverage data to update the snapshot");

                Logger.getInstance().info("Updating snapshot '" + snapshotLocation.getAbsolutePath() +
                        "' against OpenClover database at '" +  db.getInitstring() + "'");
                snapshot.updateFor(db);
            }

            start = System.currentTimeMillis();
            snapshot.store();
            Logger.getInstance().verbose("Took " + (System.currentTimeMillis() - start) + "ms to snapshot");
        } catch (Exception e) {
            Logger.getInstance().error("Failed to create or update snapshot file at " + snapshotLocation.getAbsolutePath(), e);
            throw new BuildException(e);
        }
    }

    private PerTestCoverageStrategy getPerTestCoverageStrategy() {
        final String strategy = System.getProperty(CloverNames.PROP_MEMORY_STRATEGY_SNAPSHOT,
                PerTestCoverageStrategy.IN_MEMORY.name());
        try {
            return PerTestCoverageStrategy.valueOf(strategy);
        } catch (IllegalArgumentException ex) {
            return PerTestCoverageStrategy.IN_MEMORY;
        }
    }
}
