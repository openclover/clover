package org.openclover.core.api.registry;

public interface HasVersionRanges {

    public static final long NO_VERSION = -1L;

    void addVersion(long version);

    void addVersions(long minVersion, long maxVersion);
}
