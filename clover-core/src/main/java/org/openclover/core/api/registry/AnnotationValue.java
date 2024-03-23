package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a value of an annotation attribute.
 */
public interface AnnotationValue {
    /*
     * @return the value as an array of AnnotationValues - where this is singular a list of length 1 is returned, where this
     * represents an ordered collection of values an ordered list of those values is returned.
     */
    @NotNull
    List<AnnotationValue> toList();
}
