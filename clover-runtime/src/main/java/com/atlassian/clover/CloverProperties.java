package com.atlassian.clover;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CloverProperties {
    private final Map<String, String> instrTimeProperties;

    public CloverProperties(String... instrTimeProperties) {
        this.instrTimeProperties = toMap(instrTimeProperties);
    }

    public String getProperty(final String name) {
        return getSysProperty(name, instrTimeProperties.get(name));
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        return Boolean.parseBoolean(getSysProperty(name, Boolean.toString(defaultValue)));
    }

    public static String getSysProperty(final String name, final String defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(name, defaultValue);
            }
        });
    }

    public static boolean getBooleanSysProperty(String name, boolean defaultValue) {
        return Boolean.parseBoolean(getSysProperty(name, Boolean.toString(defaultValue)));
    }

    private static Map<String, String> toMap(String[] nvpProperties) {
        Map<String, String> properties = new HashMap<>();

        if (nvpProperties != null) {
            if (nvpProperties.length % 2 != 0) {
                throw new IllegalArgumentException("The number of Clover properties strings supplied should be a multiple of 2");
            }

            for(int i = 0; i < nvpProperties.length; i += 2) {
                properties.put(nvpProperties[i], nvpProperties[i + 1]);
            }
        }
        return properties;
    }

    public static CloverProperties newEmptyProperties() {
        return new CloverProperties();
    }

    @Override
    public String toString() {
        return "[InstrumentationProperties=" + instrTimeProperties + ", cloverSystemProperties=" + getCloverSystemProperties() + "]";
    }

    private String getCloverSystemProperties() {
        Properties systemProperties = AccessController.doPrivileged(new PrivilegedAction<Properties>() {
            @Override
            public Properties run() {
                return System.getProperties();
            }
        });

        Map<String, String> cloverSystemProperties = new HashMap<>();
        for (Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
            if (((String) entry.getKey()).startsWith(CloverNames.PROP_PREFIX)) {
                cloverSystemProperties.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
        return cloverSystemProperties.toString();
    }
}
