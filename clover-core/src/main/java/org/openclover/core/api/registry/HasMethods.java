package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a code entity containing methods or their equivalent (like Java8 lambdas or Closure functions)
 */
public interface HasMethods {
    /**
     * Returns list of methods
     * @return List&lt;? extends MethodInfo&gt; - list of methods or empty list if none
     */
    @NotNull
    List<? extends MethodInfo> getMethods();

    /**
     * Returns list of all methods, including indirect descendants.
     * @return List&lt;? extends MethodInfo&gt; - list of methods or empty list if none
     */
    @NotNull
    List<? extends MethodInfo> getAllMethods();
}
