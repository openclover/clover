package org.openclover.core.api.registry;

public interface IsCacheable {

    void buildCaches();

    void invalidateCaches();
}
