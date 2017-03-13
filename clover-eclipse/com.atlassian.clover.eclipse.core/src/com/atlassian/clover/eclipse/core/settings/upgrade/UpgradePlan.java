package com.atlassian.clover.eclipse.core.settings.upgrade;

import com.atlassian.clover.eclipse.core.settings.InstallationSettings;
import com.atlassian.clover.eclipse.core.settings.WorkspaceSettings;

public interface UpgradePlan {
    public void apply(InstallationSettings installationSettings, WorkspaceSettings workspaceSettings);
}
