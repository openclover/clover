package org.openclover.eclipse.core.exclusion;

import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.swt.widgets.Display;

public class DecorationPreferenceChangeListener implements IPreferenceChangeListener {
    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        final String key = event.getKey();
        if (ProjectSettings.Keys.EXCLUDE_FILTER.equals(key)
                || ProjectSettings.Keys.INCLUDE_FILTER.equals(key)
                || ProjectSettings.Keys.INSTRUMENT_SELECTED_SOURCE_FOLDERS.equals(key)
                || key.startsWith(ProjectSettings.Keys.INSTRUMENTED_FOLDER_PATTERNS)) {
            Display.getDefault().asyncExec(() -> ExclusionLabelDecorator.decorationChanged());
        }
    }

}
