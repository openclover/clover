package com.atlassian.clover.idea.autoupdater;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class AutoUpdateConfiguration {
    private boolean autoUpdate = true;
    private boolean useMilestone;
    private List<String> ignoredVersions = newArrayList();

    public AutoUpdateConfiguration(AutoUpdateConfiguration configuration) {
        autoUpdate = configuration.autoUpdate;
        useMilestone = configuration.useMilestone;
        ignoredVersions = newArrayList(configuration.ignoredVersions);
    }

    public AutoUpdateConfiguration() {
    }

    public AutoUpdateConfiguration(boolean autoUpdate, boolean useMilestone) {
        this.autoUpdate = autoUpdate;
        this.useMilestone = useMilestone;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public boolean isUseMilestone() {
        return useMilestone;
    }

    public void setUseMilestone(boolean useMilestone) {
        this.useMilestone = useMilestone;
    }

    public List<String> getIgnoredVersions() {
        return ignoredVersions; // can't be unmodifiableCollection because it breaks IDEA deserializer. Barf.
    }

    public void setIgnoredVersions(List<String> list) {
        ignoredVersions = newArrayList(list);
    }

    public void addIgnoredVersion(String version) {
        ignoredVersions.add(version);
    }

    @SuppressWarnings("ALL") //autogenerated
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AutoUpdateConfiguration))
            return false;

        final AutoUpdateConfiguration that = (AutoUpdateConfiguration) o;

        if (autoUpdate != that.autoUpdate)
            return false;
        if (useMilestone != that.useMilestone)
            return false;
        if (!ignoredVersions.equals(that.ignoredVersions))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (autoUpdate ? 1 : 0);
        result = 31 * result + (useMilestone ? 1 : 0);
        result = 31 * result + ignoredVersions.hashCode();
        return result;
    }
}