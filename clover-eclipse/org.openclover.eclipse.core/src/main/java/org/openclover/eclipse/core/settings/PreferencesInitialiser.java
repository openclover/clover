package org.openclover.eclipse.core.settings;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Initialises preferences
 */
public class PreferencesInitialiser extends AbstractPreferenceInitializer {
    @Override
    public void initializeDefaultPreferences() {
        final Preferences node = DefaultScope.INSTANCE.getNode(CloverPlugin.ID);
        try {
            node.clear();
        } catch (BackingStoreException e) {
        }

        initialiseConfigPreferences(node);
        initialiseInstancePreferences(node);
        initialiseProjectPreferences(node);
    }

    private void initialiseInstancePreferences(Preferences node) {
        node.putBoolean(WorkspaceSettings.Keys.WORKING_SET_ENABLED, WorkspaceSettings.Defaults.DEFAULT_WORKING_SET_ENABLED);
    }

    private void initialiseConfigPreferences(Preferences node) {
        node.putBoolean(InstallationSettings.Keys.WHEN_REBUILDING_PROMPT_ME, InstallationSettings.Defaults.WHEN_REBUILDING_PROMPT_ME);
        node.put(InstallationSettings.Keys.ACTION_WHEN_REBUILDING, InstallationSettings.Defaults.ACTION_WHEN_REBUILDING);
        node.putBoolean(InstallationSettings.Keys.WHEN_CONTEXT_CHANGES_PROMPT_ME, InstallationSettings.Defaults.WHEN_CONTEXT_CHANGE_PROMPT_ME);
        node.put(InstallationSettings.Keys.ACTION_WHEN_CONTEXT_CHANGES, InstallationSettings.Defaults.ACTION_WHEN_CONTEXT_CHANGES);
        node.putBoolean(InstallationSettings.Keys.WHEN_INSTRUMENTATION_SOURCE_CHANGES_PROMPT_ME, InstallationSettings.Defaults.WHEN_INSTRUMENTATION_SOURCE_CHANGE_PROMPT_ME);
        node.put(InstallationSettings.Keys.ACTION_WHEN_INSTRUMENTATION_SOURCE_CHANGES, InstallationSettings.Defaults.ACTION_WHEN_INSTRUMENTATION_SOURCE_CHANGES);
        node.putLong(InstallationSettings.Keys.COVERAGE_REFRESH_PERIOD, InstallationSettings.Defaults.COVERAGE_REFRESH_PERIOD);
        node.putBoolean(InstallationSettings.Keys.AUTO_REFRESH_COVERAGE_DATA, true);
        node.put(InstallationSettings.Keys.COVERAGE_SPAN, InstallationSettings.Defaults.COVERAGE_SPAN);
        node.putInt(InstallationSettings.Keys.COVERAGE_STYLE_IN_EDITORS, InstallationSettings.Defaults.SHOW_COVERAGE_IN_EDITORS);
        node.put(InstallationSettings.Keys.LOGGING_LEVEL, InstallationSettings.Defaults.LOGGING_LEVEL);
        //Java 1.4 under OSX has SWT/AWT interoperability issues which cause the
        //JVM to crash when generating HTML reports
        node.putBoolean(InstallationSettings.Keys.PRIME_AWT_IN_UI_THREAD, InstallationSettings.Defaults.PRIME_AWT_IN_UI_THREAD);
        node.putBoolean(InstallationSettings.Keys.AGGREGATE_COVERAGE, InstallationSettings.Defaults.AGGREGATE_COVERAGE);
        node.putBoolean(InstallationSettings.Keys.TRACK_PERTEST_COVERAGE, InstallationSettings.Defaults.TRACK_PERTEST_COVERAGE);
        node.putBoolean(InstallationSettings.Keys.PERTEST_COVERAGE_INMEM, InstallationSettings.Defaults.PERTEST_COVERAGE_INMEM);
        node.putBoolean(InstallationSettings.Keys.SHOW_EXCLUSION_ANNOTATIONS, InstallationSettings.Defaults.SHOW_EXCLUSION_ANNOTATIONS);
        node.putBoolean(InstallationSettings.Keys.INCLUDE_FAILED_COVERAGE, InstallationSettings.Defaults.INCLUDE_FAILED_COVERAGE);
        node.putBoolean(InstallationSettings.Keys.PRESERVE_INSTRUMENTED_SOURCES, InstallationSettings.Defaults.PRESERVE_INSTRUMENTED_SOURCES);
        node.putBoolean(InstallationSettings.Keys.AUTO_OPEN_CLOVER_VIEWS, InstallationSettings.Defaults.AUTO_OPEN_CLOVER_VIEWS);
    }

    private void initialiseProjectPreferences(Preferences node) {
        node.putBoolean(ProjectSettings.Keys.INSTRUMENTATION_ENABLED, true);
        node.putBoolean(ProjectSettings.Keys.OUTPUT_ROOT_SAME_AS_PROJECT, true);
        node.putBoolean(ProjectSettings.Keys.INIT_STRING_DEFAULT, true);
        node.putBoolean(ProjectSettings.Keys.INIT_STRING_PROJECT_RELATIVE, true);
        node.put(ProjectSettings.Keys.INIT_STRING, ProjectSettings.Defaults.USER_INITSTRING);
        node.putInt(ProjectSettings.Keys.FLUSH_INTERVAL, ProjectSettings.Defaults.FLUSH_INTERVAL);
        node.putBoolean(ProjectSettings.Keys.SHOULD_QUALIFY_JAVA_LANG, ProjectSettings.Defaults.SHOULD_QUALIFY_JAVA_LANG);
        node.putBoolean(ProjectSettings.Keys.USING_DEFAULT_TEST_DETECTION, ProjectSettings.Defaults.USING_DEFAULT_TEST_DETECTION);
        node.put(ProjectSettings.Keys.OUTPUT_ROOT, ProjectSettings.Defaults.USER_OUTPUT_DIR);
        node.putBoolean(ProjectSettings.Keys.RECREATE_OUTPUT_DIRS, ProjectSettings.Defaults.RECREATE_OUTPUT_DIRS);
        node.putBoolean(ProjectSettings.Keys.OUTPUT_ROOT_SAME_AS_PROJECT, ProjectSettings.Defaults.OUTPUT_ROOT_SAME_AS_PROJECT);
        node.putInt(ProjectSettings.Keys.TEST_SOURCE_FOLDERS, ProjectSettings.Defaults.TEST_SOURCE_FOLDERS);
    }
}
