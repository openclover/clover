package org.openclover.eclipse.core.settings.upgrade;

import org.openclover.eclipse.core.settings.InstallationSettings;
import org.openclover.eclipse.core.settings.WorkspaceSettings;

public interface UpgradePlan {
    void apply(InstallationSettings installationSettings, WorkspaceSettings workspaceSettings);
}
