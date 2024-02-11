package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a code entity containing branches or their equivalent (if-then-else, switch blocks etc)
 */
public interface HasBranches {
    /**
     * Returns list of branches
     * @return List&lt;? extends BranchInfo&gt; - list of branches or empty list if none
     */
    @NotNull
    List<? extends BranchInfo> getBranches();

}
