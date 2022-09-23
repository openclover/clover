package com.atlassian.clover.optimization;

import com.atlassian.clover.api.optimization.Optimizable;

import java.util.List;
import java.util.Collection;

/**
 * Interface for implementations that can optimize a collection of {@link com.atlassian.clover.api.optimization.Optimizable}
 * objects. Optimization is defined as one or both of:
 * <ul/>
 * <li>Eliminating an object from the supplied lists (by not returning it) - called minimization;
 * <li>Reordering the list of objects supplied in some beneficial way.
 * </ul>
 */
public interface Optimizer {
    /**
     * Optimizes a {@link Collection} of {@link Optimizable} objects.
     */
    <E extends Optimizable> List<E> optimize(Collection<E> optimizables);

    /**
     * Optimizes a {@link Collection} of {@link com.atlassian.clover.api.optimization.Optimizable} objects.
     */
    <E extends Optimizable> List<E> optimize(
        Collection<E> optimizables,
        OptimizationSession session);

    /**
     * Optimizes two {@link Collection}s of {@link Optimizable} objects where
     * the first {@link Collection} must not be minimzed.
     */
    <E extends Optimizable> List<E> optimize(
        Collection<E> mandatoryOptimizables,
        Collection<E> optionalOptimizables);

    /**
     * Optimizes two {@link Collection}s of {@link Optimizable} objects where
     * the first {@link Collection} must not be minimzed.
     */
    <E extends Optimizable> List<E> optimize(
        Collection<E> mandatoryOptimizables,
        Collection<E> optionalOptimizables,
        OptimizationSession session);

    /**
     * Determines if an {@link Optimizable} object should be included in optimized
     * output or not - used by clients where the full list of {@link Optimizable}
     * objects to be optimized can not be calculated up-front.
     * @param optimizable the subject of the query
     * @param session stores incremental information about optimization and can be later used
     *        for debug/logging summary
     */
    <E extends Optimizable> boolean include(E optimizable, OptimizationSession session);

    /**
     * Determines if the Optimizer is in a state where it can optimize. If it
     * is not able to optimize, {@link #include(Optimizable, OptimizationSession)} will return
     * true and both {@link #optimize(java.util.Collection)} and {@link #optimize(java.util.Collection, java.util.Collection)}
     * will return all supplied {@link Optimizable} objects supplied to it.
     **/
    boolean canOptimize();
}
