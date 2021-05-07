package org.openclover.eclipse.testopt;

import org.eclipse.core.runtime.IStatus;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class TestOptimizationPluginMessages {
    private static final String BUNDLE_NAME = "plugin";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private TestOptimizationPluginMessages() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            TestOptimizationPlugin.log(IStatus.WARNING, "Missing string resource: " + key, e);
            return "!" + key + "!";
        }
    }

    public static String getFormattedString(String key, Object arg) {
        String format;
        try {
            format = RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            TestOptimizationPlugin.log(IStatus.WARNING, "Missing string resource: " + key, e);
            return "!" + key + "!";
        }
        if (arg == null)
            arg = "";
        return MessageFormat.format(format, (arg instanceof Object[]) ? (Object[])arg : new Object[]{arg});
    }

    public static String getPluginName() {
        return getString("pluginName");
    }

}
