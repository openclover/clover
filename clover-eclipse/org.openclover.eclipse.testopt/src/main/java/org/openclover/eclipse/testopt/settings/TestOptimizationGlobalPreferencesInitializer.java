package org.openclover.eclipse.testopt.settings;

import org.openclover.core.api.optimization.OptimizationOptions;
import org.openclover.eclipse.testopt.TestOptimizationPlugin;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class TestOptimizationGlobalPreferencesInitializer extends AbstractPreferenceInitializer {
    @Override
    public void initializeDefaultPreferences() {
        final Preferences node = DefaultScope.INSTANCE.getNode(TestOptimizationPlugin.ID);
        try {
            node.clear();
        } catch (BackingStoreException e) {
            //ignore
        }
        node.putBoolean(TestOptimizationPlugin.SHOW_NO_TESTS_FOUND_DIALOG, true);
        node.putBoolean(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS, true);
        node.putInt(TestOptimizationPlugin.DISCARD_STALE_SNAPSHOTS_AGE, 10);
        node.putBoolean(TestOptimizationPlugin.MINIMIZE_TESTS, true);
        node.put(TestOptimizationPlugin.TEST_REORDERING, OptimizationOptions.TestSortOrder.FAILFAST.name());
    }
}
