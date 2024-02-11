package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a code entity containing classes or their equivalent (like interfaces in Java, objects in Scala etc)
 */
public interface HasClasses {
    /**
     * Returns list of classes
     *
     * @return List&lt;? extends ClassInfo&gt; - list of classes or empty list if none
     */
    @NotNull
    List<? extends ClassInfo> getClasses();

    /**
     * Returns list of all classes, including indirect descendants.
     * @return List&lt;? extends ClassInfo&gt; - list of classes or empty list if none
     */
    @NotNull
    List<? extends ClassInfo> getAllClasses();
}
