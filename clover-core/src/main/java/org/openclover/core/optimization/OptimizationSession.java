package org.openclover.core.optimization;

import org.openclover.core.api.optimization.OptimizationOptions;
import org.openclover.core.cfg.Interval;
import org.openclover.core.util.Color;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.Formatting;

import java.util.Set;
import java.util.Collections;

import static org.openclover.core.util.Sets.newHashSet;

public class OptimizationSession {
    private OptimizationOptions optimizationOptions;
    private long savingsInMs;
    private long totalTimeInMs;
    private int originalTestableCount;
    private int optimizedTestableCount;
    private int foundTestableCount;
    private boolean autoSummarize;
    private final Set<String> modifiedFiles = newHashSet();
    private boolean triedToOptimize;

    public OptimizationSession(OptimizationOptions optimizationOptions, boolean autoSummarize) {
        this.optimizationOptions = optimizationOptions;
        this.autoSummarize = autoSummarize;
    }

    public OptimizationSession(OptimizationOptions options) {
        this(options, true);
    }

    void incTotalTime(long inMs) {
        totalTimeInMs += inMs;
    }

    void incSavings(long inMs) {
        savingsInMs += inMs;
    }

    void incFoundOptimizableCount(int i) {
        foundTestableCount++;
    }

    void incOriginalOptimizableCount(int count) {
        originalTestableCount += count;
    }

    void incOptimizedOptimizableCount(int count) {
        optimizedTestableCount += count;
    }

    void addModifiedPath(String path) {
        modifiedFiles.add(path);
    }

    public Set getOptimizedPaths() {
        return Collections.unmodifiableSet(modifiedFiles);
    }

    void afterOptimizaion(boolean triedToOptimize) {
        this.triedToOptimize |= triedToOptimize;
        if (autoSummarize) {
            summarize();
        }
    }

    public void summarize() {
        if (savingsInMs <= 0) {
            Logger.getInstance().info(
                "Clover " + (autoSummarize ? "was" : "is") + " unable to save any time on this optimized test run.");
        } else {

            Logger.getInstance().info(Color.make(
                    "Clover estimates " + (autoSummarize ? "having saved" : "saving") + " around "
                            + new Interval(Math.max(1000, savingsInMs) / 1000, Interval.UNIT_SECOND).toSensibleString()
                            + " on this optimized test run. ").b().green() + fullTestRunMsg());
        }

        final String pluralTestKind = Formatting.pluralizedWord(2, optimizationOptions.getOptimizableName());
        Logger.getInstance().info(
        "Clover " + (autoSummarize ? "included " : "is including ")
            + Color.make(
                optimizedTestableCount + " test "
                + Formatting.pluralizedWord(optimizedTestableCount, optimizationOptions.getOptimizableName())).b()
            + " in this run (total # test " + pluralTestKind + " : " + originalTestableCount + ")");
        Logger.getInstance().verbose(
            "Clover matched " + foundTestableCount + " of your " + originalTestableCount + " test " + pluralTestKind +
            " with those in the snapshot (optimization heuristic was applied to them)." +
            ((foundTestableCount < originalTestableCount)
                ? " Unmatched " + pluralTestKind + " either means Clover has a bug, your build is misconfigured for test optimization or you have not instrumented your test source with Clover."
                : ""));
    }

    private String fullTestRunMsg() {
        final String fulltestRunMsg = "The full test run takes approx. " +
                new Interval(Math.max(1000, totalTimeInMs) / 1000, Interval.UNIT_SECOND).toSensibleString();
        return fulltestRunMsg;
    }

    /**
     * Used by the plugins.
     * @return popup notification
     */
    public String getPlainSummary() {
        StringBuilder sb = new StringBuilder();
        //disabled until Clover is able to compute these times properly
//        if (savingsInMs <= 0) {
//            sb.append("Clover " + (autoSummarize ? "was" : "is") + " unable to save any time on this optimized test run.");
//        } else {
//            sb.append(
//                "Clover estimates " + (autoSummarize ? "having saved" : "saving") + " around "
//                + new Interval(Math.max(1000, savingsInMs)/1000, Interval.UNIT_SECOND).toSensibleString()
//                + " on this optimized test run. ").append(fullTestRunMsg());
//        }
//        sb.append('\n');

        final String pluralTestKind = Formatting.pluralizedWord(2, optimizationOptions.getOptimizableName());
        sb.append("Clover ")
                .append(autoSummarize ? "included " : "is including ")
                .append(optimizedTestableCount)
                .append(" test ")
                .append(Formatting.pluralizedWord(optimizedTestableCount, optimizationOptions.getOptimizableName()))
                .append(" in this run (total # test ")
                .append(pluralTestKind)
                .append(" : ")
                .append(originalTestableCount)
                .append(")")
                .append('\n');

        if (foundTestableCount < originalTestableCount) {
            sb.append("Clover matched ")
                    .append(foundTestableCount)
                    .append(" of your ")
                    .append(originalTestableCount)
                    .append(" test ")
                    .append(pluralTestKind)
                    .append(" with those registered during previous test runs (optimization heuristic was applied to them).\n")
                    .append(" Unmatched ")
                    .append(pluralTestKind)
                    .append(" may mean your build is misconfigured for test optimization or you have not instrumented your test source with Clover.");
        }
        return sb.toString();
    }

    public OptimizationOptions getOptimizationOptions() {
        return optimizationOptions;
    }

    public long getSavingsInMs() {
        return savingsInMs;
    }

    public int getOriginalTestableCount() {
        return originalTestableCount;
    }

    public int getOptimizedTestableCount() {
        return optimizedTestableCount;
    }

    public int getFoundTestableCount() {
        return foundTestableCount;
    }

    public boolean isAutoSummarize() {
        return autoSummarize;
    }
}
