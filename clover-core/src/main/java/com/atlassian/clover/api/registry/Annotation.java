package com.atlassian.clover.api.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents an annotation.
 */
public interface Annotation {

    @Nullable
    AnnotationValue getAttribute(@NotNull String attributeName);

    /**
     * Returns map of attributes for this annotation (or an empty map if no attribute is defined)
     * @return Map&lt;String, AnnotationValue&gt; attributes
     */
    @NotNull
    Map<String, AnnotationValue> getAttributes();

    @NotNull
    String getName();

    void setName(@NotNull String annotationName);

}
