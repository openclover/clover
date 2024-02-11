package org.openclover.core.api.registry;

/**
 * Represents a code entity, which can have a context filter defined for metrics.
 */
public interface HasContextFilter {
    /**
     * Returns context filter for the entity, i.e set of custom statement/method contexts as well as Clover's built-in
     * code contexts.
     *
     * @return ContextSet
     */
    ContextSet getContextFilter();
}
