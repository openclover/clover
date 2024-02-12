package org.openclover.runtime.remote;

import org.openclover.runtime.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;


/**
 * A simple class to convert an init String to a map, and vice-versa.
 */
public class InitStringData implements Serializable {

    private final Map<String, String> initStringMap;

    public InitStringData(String initString) {

        this.initStringMap = new HashMap<>();

        final StringTokenizer tok = new StringTokenizer(initString, ";");

        while (tok.hasMoreTokens()) {
            // split and trim on the '='
            final String keyValue = tok.nextToken();
            final String[] pair = keyValue.split("=");
            if (pair.length != 2) {
                throw new IllegalArgumentException("Invalid key=value pair, '" + keyValue + "'  for initString: " + initString);
            }
            initStringMap.put(pair[0].trim(), pair[1].trim());
        }

    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        final String value = initStringMap.get(key);
        return value == null ? defaultValue : value;
    }

    public int get(String key, int defaultValue) {
        final String value = initStringMap.get(key);
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Logger.getInstance().warn("Value for key: " + key + " not a number: " + value + " - " + e.getMessage());
        }
        return defaultValue;
    }

    public boolean get(String key, boolean defaultValue) {
        final String value = initStringMap.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }
    
    public void set(String key, boolean value) {
        initStringMap.put(key, Boolean.toString(value));
    }

    public void set(String key, String value) {
        initStringMap.put(key, value);
    }

    public void set(String key, int value) {
        set(key, Integer.toString(value));
    }

    /**
     * Return "key1=value1;key2=value2;..." sorted alphabetically by key or
     * return an empty string "" if there are no map entries.
     * @return String
     */
    @Override
    public String toString() {
        final SortedSet<String> keys = new TreeSet<>(initStringMap.keySet());
        StringBuilder initString = new StringBuilder();
        for (String key : keys) {
            initString.append(initString.length() > 0 ? ";" : "").append(key).append("=").append(initStringMap.get(key));
        }
        return initString.toString();
    }


}
