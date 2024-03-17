package org.openclover.core.optimization;

import org.openclover.core.util.Color;
import org_openclover_runtime.CloverVersionInfo;

public class Messages {
    public static String noOptimizationBecauseNoRegistryFound(String initString) {
        return Color.make(
            "OpenClover is unable to optimize this test run because no database was not found at: '" + initString + "'"
        ).b().red().toString();
    }

    public static String noOptimizationBecauseNoSnapshotFound(String path) {
        return Color.make(
            "OpenClover is not optimizing this test run as no test snapshot file was found at '"
                + path + "'."
        ).b().red().toString();
    }

    public static String noOptimizationBecauseOfException(Exception e) {
        return Color.make(
            "OpenClover is unable to optimize this test run due to an exception: " + e.getMessage()
        ).b().red().toString();
    }

    public static String noOptimizationBecauseInaccurate(int fullRunEvery, int dbVersionCount) {
        return Color.make(
            "OpenClover is not optimizing this test run so as to increase the accuracy of subsequent optimized runs (threshold of " + fullRunEvery +
                    " consecutive optimized builds met). Total number of builds so far: " + dbVersionCount 
        ).green().toString();
    }

    public static String noOptimizationBecauseOldVersion(String oldVersionInfo) {
        return Color.make("OpenClover can not optimize this test run because it was upgraded since the last build: current version = \""
                + CloverVersionInfo.formatVersionInfo() + "\", previous version = \"" + oldVersionInfo + "\"").green().toString();
    }

    public static String loadedSnapshotFrom(String path) {
        return Color.make(
            "Loaded snapshot from: '" + path + "'."
        ).green().toString();
    }
}