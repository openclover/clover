package org.openclover.core.api.registry;

/**
 * Represents an entity which can be versioned.
 */
public interface HasVersions {

    long getVersion();

    void setVersion(long version);
}
