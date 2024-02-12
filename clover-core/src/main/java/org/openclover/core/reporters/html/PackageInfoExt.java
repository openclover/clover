package org.openclover.core.reporters.html;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.PackageInfo;

/**
 * Helper class holding {@link PackageInfo} and boolean whether it's a
 * test-only package (i.e. existing only in {@link CloverDatabase#getTestOnlyModel()}).
 */
public class PackageInfoExt {
    private final PackageInfo packageInfo;
    private final boolean isTestOnly;

    public PackageInfoExt(@NotNull PackageInfo packageInfo, boolean isTestOnly) {
        this.packageInfo = packageInfo;
        this.isTestOnly = isTestOnly;
    }

    @NotNull
    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    @SuppressWarnings("unused") // see aui-vertical-nav.vm + all-pkgs.vm
    public boolean isTestOnly() {
        return isTestOnly;
    }

    @Override
    public String toString() {
        return packageInfo.getName() + " " + isTestOnly;
    }
}
