package com.atlassian.clover.api.optimization;

import org.openclover.runtime.Logger;
import com.atlassian.clover.optimization.Snapshot;

import java.io.File;

/**
 */
public class OptimizationOptions {

    public enum TestSortOrder {
        NONE(0), FAILFAST(1), RANDOM(2);

        private final int integer;

        TestSortOrder(int integer) {
            this.integer = integer;
        }

        public int asInteger() {
            return integer;
        }
    }


    private final boolean enabled;
    private final boolean minimize;
    private final TestSortOrder reorder;
    private final boolean debug;
    private final int maxCompilesBeforeStaleSnapshot;
    private final Logger logger;
    private final String optimizableName;
    private final File snapshot;
    private final String initString;

    private OptimizationOptions(Builder builder) {
        this.enabled = builder.enabled;
        this.minimize = builder.minimize;
        this.reorder = builder.reorder;
        this.debug = builder.debugFlag;
        this.maxCompilesBeforeStaleSnapshot = builder.maxCompilesBeforeStaleSnapshot;
        this.logger = builder.logger;
        this.optimizableName = builder.optimizableName;
        this.snapshot = builder.snapshot;
        this.initString = builder.initString;
    }

    public boolean isReorderFailfast() {
        return getReorder() == TestSortOrder.FAILFAST;
    }

    public boolean isReordering() {
        return getReorder() != TestSortOrder.NONE;
    }

    public boolean isReorderRandomly() {
        return getReorder() == TestSortOrder.RANDOM;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMinimize() {
        return minimize;
    }

    public String getOptimizableName() {
        return optimizableName;
    }

    public TestSortOrder getReorder() {
        return reorder;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getMaxCompilesBeforeStaleSnapshot() {
        return maxCompilesBeforeStaleSnapshot;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getInitString() {
        return initString;
    }

    public File getSnapshotFile() {
        return snapshot;
    }

    @Override
    public String toString() {
        return "OptimizationOptions{" +
                "enabled=" + enabled +
                ", minimize=" + minimize +
                ", reorder=" + reorder +
                ", debug=" + debug +
                ", maxCompilesBeforeStaleSnapshot=" + maxCompilesBeforeStaleSnapshot +
                ", optimizableName='" + optimizableName + '\'' +
                ", initString='" + initString + '\'' +
                ", snapshot=" + snapshot +
                '}';
    }

    /**
     * Options for use with the clover {@link TestOptimizer} class.
     * This class uses the Builder Pattern with a fluent style.
     * For example, to configure the TestOptimizer to use a
     * snapshot file in /tmp/clover.snapshot, and to not perform reordering, you would create an Options instance like so:
     * <pre>
     * Options options = new Options.Builder().snapshot(new File(".clover/clover.snapshot")).dontReorder().build();
     * </pre>
     */
    public static final class Builder {

        private boolean enabled;
        private boolean minimize;
        private OptimizationOptions.TestSortOrder reorder;
        private boolean debugFlag;
        private int maxCompilesBeforeStaleSnapshot;
        private Logger logger;
        private String optimizableName;
        private File snapshot;
        private String initString;

        public Builder() {

            this.enabled(true)
                    .minimize(true)
                    .reorderFailfast()
                    .optimizableName("test")
                    .maxCompilesBeforeStaleSnapshot(10).debug(false);

        }


        public Builder snapshot(File snapshot) {
            this.snapshot = snapshot;
            return this;
        }

        public Builder initString(String initString) {
            this.initString = initString;
            return this;
        }

        public Builder initStringAndSnapshotFrom(String initString) {
            return
                    initString(initString)
                            .snapshot(Snapshot.fileForInitString(initString));
        }

        public Builder optimizableName(String name) {
            this.optimizableName = name;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder minimize(boolean minimize) {
            this.minimize = minimize;
            return this;
        }

        public Builder reorderFailfast() {
            this.reorder = OptimizationOptions.TestSortOrder.FAILFAST;
            return this;
        }

        public Builder dontReorder() {
            this.reorder = OptimizationOptions.TestSortOrder.NONE;
            return this;
        }

        public Builder reorderRandomly() {
            this.reorder = OptimizationOptions.TestSortOrder.RANDOM;
            return this;
        }

        public Builder reorder(OptimizationOptions.TestSortOrder reorder) {
            this.reorder = reorder;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debugFlag = debug;
            return this;
        }

        public Builder maxCompilesBeforeStaleSnapshot(int maxCompilesBeforeStaleSnapshot) {
            this.maxCompilesBeforeStaleSnapshot = maxCompilesBeforeStaleSnapshot;
            return this;
        }

        public OptimizationOptions build() {
            return new OptimizationOptions(this);
        }
    }


}
