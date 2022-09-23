package com.atlassian.clover.registry;

/**
 * A collection of annotation values. Values may be keyed but this decision is
 * left to implementing classes.
 */
public interface AnnotationValueCollection {
    void put(String key, PersistentAnnotationValue value);
}
