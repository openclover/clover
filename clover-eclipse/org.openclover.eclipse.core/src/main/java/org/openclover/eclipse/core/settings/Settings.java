package org.openclover.eclipse.core.settings;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.openclover.eclipse.core.CloverPlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;


public abstract class Settings {
    protected IEclipsePreferences isolatedPreferences;
    protected IPreferencesService preferencesService;

    protected Settings() {
        buildPreferencesService();
    }

    private void buildPreferencesService() {
        preferencesService = Platform.getPreferencesService();
    }

    protected IScopeContext[] getScopeDelegation() {
        return null;
    }

    public void addListener(IEclipsePreferences.IPreferenceChangeListener listener) {
        ((IEclipsePreferences)getIsolatedPreferences()).addPreferenceChangeListener(listener);
    }

    public void removeListener(IEclipsePreferences.IPreferenceChangeListener listener) {
        ((IEclipsePreferences)getIsolatedPreferences()).removePreferenceChangeListener(listener);
    }

    protected Preferences getIsolatedPreferences() {
        return isolatedPreferences;
    }

    protected IPreferencesService getPreferencesService() {
        return preferencesService;
    }

    public void setValue(String key, String value) {
        if (value == null) {
            getIsolatedPreferences().remove(key);
        } else {
            getIsolatedPreferences().put(key, value);
        }
        save();
    }

    public void setValue(String key, boolean value) {
        getIsolatedPreferences().putBoolean(key, value);
        save();
    }

    public void setValue(String key, int value) {
        getIsolatedPreferences().putInt(key, value);
        save();
    }

    public void setValue(String key, long value) {
        getIsolatedPreferences().putLong(key, value);
        save();
    }

    public String getLocalValue(String key) {
        return getIsolatedPreferences().get(key, null);
    }

    public String getString(String key) {
        return getPreferencesService().getString(CloverPlugin.ID, key, null, getScopeDelegation());
    }

    public long getLong(String key, long defaultValue) {
        return getPreferencesService().getLong(CloverPlugin.ID, key, defaultValue, getScopeDelegation());
    }

    public long getLong(String key) {
        return getLong(key, -1l);
    }

    public boolean getBoolean(String key) {
        return getPreferencesService().getBoolean(CloverPlugin.ID, key, false, getScopeDelegation());
    }

    public int getInt(String key, int defaultValue) {
        return getPreferencesService().getInt(CloverPlugin.ID, key, -1, getScopeDelegation());
    }

    public int getInt(String key) {
        return getInt(key, -1);
    }

    public void save() {
        try {
            getIsolatedPreferences().flush();
        } catch (BackingStoreException e) {
            CloverPlugin.logError("Unable to save settings", e);
        }
    }

    protected void load() {
        try {
            isolatedPreferences.sync();
        } catch (BackingStoreException e) {
            CloverPlugin.logError("Unable to load preferences", e);
        }
    }
}
