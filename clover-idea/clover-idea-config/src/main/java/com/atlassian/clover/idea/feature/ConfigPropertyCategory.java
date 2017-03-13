package com.atlassian.clover.idea.feature;

import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.idea.config.ConfigChangeEvent;
import com.atlassian.clover.idea.config.ConfigChangeListener;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;

public class ConfigPropertyCategory extends AbstractCategory implements ConfigChangeListener {

    private final String key;
    private final CloverPluginConfig data;

    public ConfigPropertyCategory(String categoryName, String configKey, CloverPluginConfig configData) {
        super(categoryName);
        key = configKey;
        data = configData;
        data.addConfigChangeListener(this);
    }

    @Override
    public void setEnabled(boolean b) {
        if (b != isEnabled()) {
            data.putProperty(key, b);
            data.notifyListeners();
        }
    }

    @Override
    public boolean isEnabled() {
        try {
            // NOTE: Want to access this config property data via the CloverPluginConfig
            // interface to ensure that we pick up the default value.
            // Direct access via the getProperty(key) method will return the
            // incorrect default value of settings.

            //TODO: Make this a little more robust - support the 'get' getter.
            String methodName = "is" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
            Method getter = data.getClass().getMethod(methodName, new Class[0]);
            Object obj = getter.invoke(data, new Object[0]);
            return (Boolean) obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configChange(ConfigChangeEvent evt) {
        if (evt.hasPropertyChange(key)) {
            boolean enabled = false;
            PropertyChangeEvent pcEvent = evt.getPropertyChange(key);
            Object val = pcEvent.getNewValue();
            if (val != null && val instanceof Boolean) {
                enabled = (Boolean) val;
            }
            CategoryEvent cevt = new CategoryEvent(this, getName(), enabled);
            fireCategoryEvent(cevt);
        }
    }

}
