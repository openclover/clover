package com.atlassian.clover.optimization;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.optimization.Optimizable;
import com.atlassian.clover.api.optimization.OptimizationOptions;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.util.collections.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static clover.com.google.common.collect.Lists.newArrayList;

/** An {@link Optimizer} that uses a local {@link Snapshot} file to optimize. */
public final class LocalSnapshotOptimizer implements Optimizer {
    /** Static convenience method to optimize */
    public static <E extends Optimizable> List<E> optimize(
        Collection<E> optimizables,
        OptimizationOptions options) throws CloverException {
                                                                                           
        return new LocalSnapshotOptimizer(options).optimize(optimizables);
    }

    /** Static convenience method to optimize */
    public static <E extends Optimizable> List<E> optimize(
        Collection<E> mandatoryOptimizables,
        Collection<E> optionalOptimizables,
        OptimizationOptions options) throws CloverException {

        return new LocalSnapshotOptimizer(options).optimize(mandatoryOptimizables, optionalOptimizables);
    }

    /** Possibly null {@link Snapshot} used to calculate optimizations */
    private final Snapshot snapshot;

    /** Possibly null {@link Clover2Registry} used to calculate optimizations */
    private final Clover2Registry registry;

    /** Options for optimization */
    private final OptimizationOptions options;

    private LocalSnapshotOptimizer(Pair<Snapshot, Clover2Registry> snapshotAndReg, OptimizationOptions options) {
        this.snapshot = snapshotAndReg.first;
        this.registry = snapshotAndReg.second;
        this.options = options;
        if (options.isDebug()) {
            Snapshot.setDebug(options.isDebug());
            SnapshotPrinter.textPrint(snapshot, Logger.getInstance(), Logger.LOG_DEBUG);
        }
    }

    public LocalSnapshotOptimizer(Snapshot snapshot, Clover2Registry registry, OptimizationOptions options) {
        this(Pair.of(snapshot, registry), options);
    }

    /**
     * @param options non-null {@link com.atlassian.clover.api.optimization.OptimizationOptions} for optimization
     */
    public LocalSnapshotOptimizer(OptimizationOptions options) {
        this(snapshotAndRegistryFor(options), options);
    }

    private static Pair<Snapshot, Clover2Registry> snapshotAndRegistryFor(OptimizationOptions options) {
        File snapshotFile = options.getSnapshotFile();
        File registryFile = options.getInitString() == null ? null : new File(options.getInitString());
        Snapshot snapshot = null;
        Clover2Registry registry = null;
        try {
            snapshot = snapshotFile == null ? null : Snapshot.loadFrom(snapshotFile);
            if (snapshot != null) {
                StringBuffer tooStaleReason = new StringBuffer();
                if (snapshot.isTooStale(options.getMaxCompilesBeforeStaleSnapshot(), tooStaleReason)) {
                    Logger.getInstance().info(tooStaleReason.toString());
                    snapshot.delete();
                    snapshot = null;
                } else {
                    registry = registryFile == null ? null : Clover2Registry.fromFile(registryFile);
                    if (registry == null) {
                        snapshot = null;
                        Logger.getInstance().info(
                                Messages.noOptimizationBecauseNoRegistryFound(
                                        registryFile == null ? "<null>" : registryFile.getAbsolutePath()));
                    }
                }
            } else {
                Logger.getInstance().info(
                        Messages.noOptimizationBecauseNoSnapshotFound(
                                snapshotFile == null ? "<null>" : snapshotFile.getAbsolutePath()));
            }
        } catch (CloverException e) {
            Logger.getInstance().info(Messages.noOptimizationBecauseOfException(e));
        }
        return Pair.of(snapshot, registry);
    }

    @Override
    public <E extends Optimizable> boolean include(E optimizable, OptimizationSession session) {
        return !optimize(Collections.singleton(optimizable)).isEmpty();
    }

    @Override
    public <E extends Optimizable> List<E> optimize(Collection<E> optimizables) {
        return optimize(optimizables, new OptimizationSession(options));
    }

    @Override
    public <E extends Optimizable> List<E> optimize(Collection<E> optimizables, OptimizationSession session) {
        return optimize(Collections.<E>emptySet(), optimizables, session);
    }

    @Override
    public <E extends Optimizable> List<E> optimize(
        Collection<E> mandatoryOptimizables,
        Collection<E> optionalOptimizables) {
        return optimize(mandatoryOptimizables, optionalOptimizables, new OptimizationSession(options));
    }

    @Override
    public <E extends Optimizable> List<E> optimize(
        Collection<E> mandatoryOptimizables,
        Collection<E> optionalOptimizables,
        OptimizationSession session) {

        //Session stats
        final int totalInputsSize = mandatoryOptimizables.size() + optionalOptimizables.size();
        session.incOriginalOptimizableCount(totalInputsSize);

        //Possibly optimized Optimizables
        List<E> result;

        if (canOptimize()) {
            result = new ArrayList<>(totalInputsSize);

            final Map<E, Set<TestMethodCall>> testMethods = new HashMap<>(totalInputsSize);

            for (E optimizable : optionalOptimizables) {
                Set<TestMethodCall> testCases = lookupTestMethods(optimizable);
                //Store relationship for later
                long totalTestRunTime = 0;
                if (testCases != null) {
                    testMethods.put(optimizable, testCases);
                    session.incFoundOptimizableCount(1);
                    totalTestRunTime = snapshot.calculateDurationOf(testCases);
                    session.incTotalTime(totalTestRunTime);
                }
                final boolean included = maybeIncludeOptimizable(optimizable, testCases, session);

                if (included) {
                    Logger.getInstance().verbose("Including " + optimizable.getName() + " in optimized test run");
                    result.add(optimizable);
                } else {
                    //Session stats
                    session.incSavings(totalTestRunTime);
                    Logger.getInstance().verbose("Excluding " + optimizable.getName() + " from optimized test run");
                }
            }

            logModifiedFiles(session.getOptimizedPaths());

            for (E optimizable : mandatoryOptimizables) {
                Set<TestMethodCall> testCases = lookupTestMethods(optimizable);
                //Store relationship for later
                if (testCases != null) {
                    testMethods.put(optimizable, testCases);
                }
                //Always add mandatory Optimizables to the end result
                Logger.getInstance().verbose("Including " + optimizable.getName() + " in optimized test run");
                result.add(optimizable);
            }

            result = performReordering(result, testMethods, snapshot, session);

            //Session stats
            session.incOptimizedOptimizableCount(result.size());
            session.afterOptimizaion(true);
        } else {
            result = new ArrayList<>(totalInputsSize);
            result.addAll(mandatoryOptimizables);
            result.addAll(optionalOptimizables);

            session.incOptimizedOptimizableCount(totalInputsSize);
            session.afterOptimizaion(false);
        }
        return result;
    }

    private void logModifiedFiles(Set<String> modifiedFiles) {
        if (options.isDebug()) {
            if (!modifiedFiles.isEmpty()) {
                Logger.getInstance().info("For the current test set, Clover detected the following source files were modified: ");
                for (String fileName : modifiedFiles) {
                    Logger.getInstance().info("\t" + fileName);
                }
            } else {
                Logger.getInstance().info("For the current test set, Clover detected no modified source files.");
            }
        }
    }

    @Override
    public boolean canOptimize() {
        return
            options.isEnabled()
            && registry != null
            && snapshot != null
            && !isTooStale();
    }

    public boolean isTooStale() {
        return snapshot != null && snapshot.isTooStale(options.getMaxCompilesBeforeStaleSnapshot());
    }

    public String cannotOptimizeCause() {
        List<String> causes = newArrayList();
        if (!options.isEnabled()) {
            causes.add("optimization disabled");
        }
        if (registry == null) {
            causes.add("registry file not set");
        }
        if (snapshot == null) {
            causes.add("no snapshot file");
        } else {
            final int max = options.getMaxCompilesBeforeStaleSnapshot();
            final StringBuffer reason = new StringBuffer();
            if (snapshot.isTooStale(max, reason)) {
                causes.add(reason.toString());
            }

        }

        return causes.toString();
    }

    private boolean containsDirty(Set<TestMethodCall> tests, OptimizationSession session) {
        for (TestMethodCall test : tests) {
            if (snapshot.getFailingTests().contains(test) || snapshot.isTestAffectedByChanges(test, registry, session)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFailed(Set<TestMethodCall> tests) {
        for (TestMethodCall test : tests) {
            if (snapshot.getFailingTests().contains(test)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAffected(Set<TestMethodCall> tests, OptimizationSession session) {
        for (TestMethodCall test : tests) {
            if (snapshot.isTestAffectedByChanges(test, registry, session)) {
                return true;
            }
        }
        return false;
    }

    private boolean maybeIncludeOptimizable(
            final Optimizable optimizable,
            final Set<TestMethodCall> testsForOptimizable, OptimizationSession session) {

        //A non-null but empty testsForOptimizable implies the test is new
        if (testsForOptimizable != null) {
            //Or any tests that hit a changed file or a failed test
            //We ignore tests if we have detected no changes were made to the
            //model likely to require a test re-run
            final boolean include;
            if (options.isMinimize()) {
                if (options.isDebug()) {
                    final boolean includesFailingTest = containsFailed(testsForOptimizable);
                    final boolean isTestNeedingRerun = containsAffected(testsForOptimizable, session);
                    include = includesFailingTest || isTestNeedingRerun;

                    if (include) {
                        Logger.getInstance().debug("Including '" + optimizable.getName() + "' in the test run because it [previously failed / was affected by a code change] [" + includesFailingTest + " / " + isTestNeedingRerun + "]");
                    } else {
                        Logger.getInstance().debug("Excluding '" + optimizable.getName() + "' in the test run because it neither previously failed nor was affected by a code change");
                    }
                    return include;
                } else {
                    return containsDirty(testsForOptimizable, session);
                }
            } else {
                if (options.isDebug()) {
                    Logger.getInstance().debug("Including '" + optimizable.getName() + "' in the test run because test minimization has been turned off.");
                }
                return true;
            }
        } else {
            if (options.isDebug()) {
                Logger.getInstance().info("Including '" + optimizable.getName() + "' in the test run because Clover cannot correlate it with previously executed test method.");
            }
            return true;
        }
    }

    private Set<TestMethodCall> lookupTestMethods(Optimizable optimizable) {
        String[] structuredPath = optimizable.getName().split("/");
        final int pathCount = structuredPath.length;
        if (pathCount == 1) {
            return snapshot.lookupTests(structuredPath[0]);
        } else {
            Set<TestMethodCall> tests = null;
            StringBuilder buffer = new StringBuilder(optimizable.getName());
            for (int i = 0; i < pathCount; i++) {
                tests = snapshot.lookupTests(buffer.toString());
                if (tests != null) {
                    break;
                }
                if (i < pathCount - 1) {
                    buffer.delete(0, structuredPath[i].length() + 1);
                }
            }
            return tests;
        }
    }

    private <E extends Optimizable> List<E> performReordering(
            final List<E> optimizables,
            final Map<E, Set<TestMethodCall>> testsPerOptimizable,
            final Snapshot snapshot, OptimizationSession session) {

        List<E> result = optimizables;
        if (optimizables.size() > 1) {
            if (options.isReorderFailfast()) {
                Logger.getInstance().verbose("Sorting optimized tests for fail-fast behaviour");
                result = sort(optimizables, testsPerOptimizable, snapshot, session);
                if (options.isDebug()) {
                    Logger.getInstance().debug("Final order for tests after failfast sorting =\n" + result);
                }
            } else if (options.isReorderRandomly()) {
                Logger.getInstance().verbose("Randomly shuffling optimized tests");
                Collections.shuffle(optimizables);
                if (options.isDebug()) {
                    Logger.getInstance().debug("Final order for tests after randomized sorting =\n" + result);
                }
            }
        } else {
            Logger.getInstance().debug("Test reordering not proceeding as there is only one test");
        }

        return result;
    }

    private <E extends Optimizable> List<E> sort(
            final List<E> optimizables,
            final Map<E, Set<TestMethodCall>> testsPerOptimizable,
            final Snapshot snapshot, OptimizationSession session) {

        final List<TestSortEntry<E>> sortedTests = new ArrayList<>(optimizables.size());
        
        for (E optimizable : optimizables) {
            final Set<TestMethodCall> tests = testsPerOptimizable.get(optimizable);
            sortedTests.add(tests != null ?
                    new TestSortEntry<>(optimizable, snapshot.calculateDurationOf(tests), !containsFailed(tests), containsAffected(tests, session))
                    : new TestSortEntry<>(optimizable, Snapshot.UNKNOWN_DURATION, true, true));
        }
        Collections.sort(sortedTests);

        final List<E> sortedOptimizables = new ArrayList<>(sortedTests.size());
        for (final TestSortEntry<E> sortedTest : sortedTests) {
            sortedOptimizables.add(sortedTest.optimizable);
        }

        return sortedOptimizables;
    }

    private class TestSortEntry<E extends Optimizable> implements Comparable<TestSortEntry<E>> {
        private E optimizable;
        private long duration;
        private boolean succeeded;
        private boolean affectedByChanges;

        private TestSortEntry(E optimizable, long duration, boolean succeeded, boolean affectedByChanges) {
            this.optimizable = optimizable;
            this.duration = duration;
            this.succeeded = succeeded;
            this.affectedByChanges = affectedByChanges;
        }

        /**
         * Failures over success first, affected over not affected second, shorter duration over longer duration third.
         * Where duration is unknown (duration == -1), we assume it took a very long time
         * and order it last.
         **/
        @Override
        public int compareTo(TestSortEntry<E> other) {
            if (succeeded == other.succeeded) {
                //If both succeeded or failed
                if (affectedByChanges == other.affectedByChanges) {
                    //If both were affected or not affected by changes
                    long duration = this.duration == -1 ? Integer.MAX_VALUE : this.duration;
                    long otherDuration = other.duration == -1 ? Integer.MAX_VALUE : other.duration;
                    return (int)(duration - otherDuration);
                } else if (affectedByChanges && !other.affectedByChanges) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (!succeeded && other.succeeded) {
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        public String toString() {
            return
                "[test='" + optimizable.getName()
                + "', duration=" + duration
                + "ms, passed=" + succeeded
                + ", affectedByChanges="
                + affectedByChanges + "]";
        }
    }
}