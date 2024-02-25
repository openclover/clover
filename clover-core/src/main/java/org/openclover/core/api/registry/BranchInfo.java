package org.openclover.core.api.registry;

/**
 *
 */
public interface BranchInfo extends ElementInfo, InstrumentationInfo, SourceInfo, HasParent {
    /**
     *
     * @param filter the {@link ContextSet} whereby Contexts to be filtered out are set to 1,
     * and preserved contexts are 0.
     * @return <code>true</code> if this element info is filtered out. i.e. excluded
     */
    boolean isFiltered(ContextSet filter);

    /**
     * Returns number of hits for the true condition branch.
     *
     * @return int hit count
     */
    int getTrueHitCount();

    /**
     * Returns number of hits for the false condition branch.
     *
     * @return int hit count
     */
    int getFalseHitCount();

    /**
     * Whether branch was instrumented or not (due to an assignment in the expression).
     *
     * @return boolean - true if instrumented, false otherwise
     */
    boolean isInstrumented();

    BranchInfo copy(MethodInfo method);

    void setContainingMethod(MethodInfo methodInfo);
}
