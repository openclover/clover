package com.atlassian.clover.eclipse.core.settings;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class WorkspaceSettings extends Settings {
    public static class Keys {
        public static final String WORKING_SET_ENABLED = "working_set_enabled";
        public static final String REPORT_HISTORY_COUNT = "report_history_count";
        public static final String REPORT_HISTORY_PREFIX = "report_history_";
    }

    public static class Defaults {
        public static final boolean DEFAULT_WORKING_SET_ENABLED = false;
    }

    public WorkspaceSettings() {
        isolatedPreferences = (IEclipsePreferences)Platform.getPreferencesService().getRootNode().node(InstanceScope.SCOPE).node(CloverPlugin.ID);
    }
}
