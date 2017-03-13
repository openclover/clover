package com.atlassian.clover.reporters.html;

import com.atlassian.clover.api.registry.PackageInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class holding {@link com.atlassian.clover.api.registry.PackageInfo} and boolean whether it's a
 * test-only package (i.e. existing only in {@link com.atlassian.clover.CloverDatabase#getTestOnlyModel()}).
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
