package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a code entity containing packages or their equivalent (like C++ namespaces)
 */
public interface HasPackages {

    /**
     * Returns list of all packages defined in the project (including nested packages). It may also contain the default
     * package - see {@link PackageInfo#DEFAULT_PACKAGE_NAME}.
     *
     * @return List&lt;PackageInfo&gt; list of packages or empty list if none
     */
    @NotNull
    List<PackageInfo> getAllPackages();

    /**
     * Searches for a package with given name
     *
     * @param name fully qualified package name
     * @return PackageInfo package found or <code>null</code>
     */
    @Nullable
    PackageInfo findPackage(String name);

}
