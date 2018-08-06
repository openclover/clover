package com.atlassian.clover.idea.config;

import com.atlassian.clover.idea.util.ComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static clover.com.google.common.collect.Lists.newArrayList;

public abstract class AbstractCloverPluginConfig implements CloverPluginConfig {

    private final Collection<ConfigChangeListener> listeners = newArrayList();

    private static class PropertyChangeEventList {
        private List<PropertyChangeEvent> propertyChanges = newArrayList();

        synchronized List<PropertyChangeEvent> detachEvents() {
            final List<PropertyChangeEvent> copy = propertyChanges;
            propertyChanges = newArrayList();
            return copy;
        }

        synchronized void addEvent(PropertyChangeEvent event) {
            // is there an existing instance of this property being changed? if
            // so, remove it.
            final Iterator<PropertyChangeEvent> i = propertyChanges.iterator();
            while (i.hasNext()) {
                PropertyChangeEvent evt = i.next();
                if (evt.getPropertyName().equals(event.getPropertyName())) {
                    i.remove();
                    // actually, the event.oldValue should be updated with this value.
                }
            }

            // a property change event should be generated.
            propertyChanges.add(event);
        }
    }

    private final PropertyChangeEventList propertyChangeEventList = new PropertyChangeEventList();

    private boolean dirty;

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty(boolean b) {
        dirty = b;
    }

    /**
     * @param l ConfigChangeListener
     */
    @Override
    public void addConfigChangeListener(ConfigChangeListener l) {
        synchronized (listeners) {
            if (!listeners.contains(l)) {
                listeners.add(l);
            }
        }
    }

    /**
     * @param l ConfigChangeListener
     */
    @Override
    public void removeConfigChangeListener(ConfigChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    @Override
    public void notifyListeners() {
        final List<PropertyChangeEvent> copy = propertyChangeEventList.detachEvents();
        fireConfigChangeEvent(new ConfigChangeEvent(this, copy));
    }

    public void fireConfigChangeEvent(ConfigChangeEvent evt) {
        // iterate over the copy, since some of the event listeners may decide
        // they are no longer interested in being listeners.... result would be
        // a java.util.ConcurrentModificationException
        final List<ConfigChangeListener> copy;
        synchronized (listeners) {
            copy = newArrayList(listeners);
        }
        for (ConfigChangeListener listener : copy) {
            listener.configChange(evt);
        }
    }

    @Override
    public void putProperty(String name, Object newValue) {
        // compare configs values. ignore same values.
        Object oldValue = getProperty(name);
        if (ComparatorUtil.areEqual(oldValue, newValue)) {
            return;
        }
        PropertyChangeEvent evt = new PropertyChangeEvent(this, name, oldValue, newValue);
        propertyChangeEventList.addEvent(evt);
        // write new properties to storage.
        markDirty(true);
        writeProperty(evt.getPropertyName(), evt.getNewValue());
    }

    /**
     * Helper method, converts the boolean primitive into a Boolean object
     * before setting the property.
     *
     * @param name String
     * @param b    boolean
     */
    public void putProperty(String name, boolean b) {
        putProperty(name, Boolean.valueOf(b));
    }

    /**
     * Helper method, converts the int primitive into an Integer object before
     * setting the property.
     *
     * @param name String
     * @param i    int
     */
    public void putProperty(String name, int i) {
        putProperty(name, new Integer(i));
    }

    /**
     * Helper method, converts the long primitive into a Long object before
     * setting the property.
     *
     * @param name String
     * @param l    long
     */
    public void putProperty(String name, long l) {
        putProperty(name, new Long(l));
    }

    /**
     * Helper method, converts the float primitive into a Float object before
     * setting the property.
     *
     * @param name String
     * @param f    float
     */
    public void putProperty(String name, float f) {
        putProperty(name, new Float(f));
    }

    /**
     * Helper method, converts the double primitive into a Double object before
     * setting the property.
     *
     * @param name String
     * @param d    double
     */
    public void putProperty(String name, double d) {
        putProperty(name, new Double(d));
    }

    /**
     * Helper method, converts the byte primitive into a Byte object before
     * setting the property.
     *
     * @param name String
     * @param b    byte
     */
    public void putProperty(String name, byte b) {
        putProperty(name, new Byte(b));
    }

    /**
     * Helper method, converts the short primitive into a Short object before
     * setting the property.
     *
     * @param name String
     * @param s    short
     */
    public void putProperty(String name, short s) {
        putProperty(name, new Short(s));
    }

    /**
     * Abstract method to be implemented by concrete subclass. The writeProperty
     * method should write the property into persistance storage.
     *
     * @param name  String
     * @param value Object
     */
    public abstract void writeProperty(String name, Object value);

    /**
     * Abstract method to be implemented by concrete subclass. The readProperty
     * method should read the property from persistance storage.
     *
     * @param name String
     * @return Object
     */
    @Nullable
    public abstract Object readProperty(String name);

    @Override
    @Nullable
    public Object getProperty(String name) {
        return readProperty(name);
    }

    @NotNull
    public Object getProperty(String name, @NotNull Object defaultValue) {
        Object val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        Object val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return (Boolean) val;
    }

    public int getIntProperty(String name, int defaultValue) {
        Object val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return (Integer) val;
    }

    public long getLongProperty(String name, long defaultValue) {
        Object val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return (Long) val;
    }

    public static final String DEFAULT_LANGUAGE_LEVEL = LANGUAGE_LEVEL_JAVA_19;

    protected final Map<String, String> languageLevel2VersionString = new HashMap<String, String>() {{
        put(LANGUAGE_LEVEL_JAVA_13, "1.3");
        put(LANGUAGE_LEVEL_JAVA_14, "1.4");
        put(LANGUAGE_LEVEL_JAVA_15, "1.5");
        // LANGUAGE_LEVEL_JAVA_16 is missing
        put(LANGUAGE_LEVEL_JAVA_17, "1.7");
        put(LANGUAGE_LEVEL_JAVA_18, "1.8");
        put(LANGUAGE_LEVEL_JAVA_19, "9");
    }};

    /**
     * Returns Java language level as a short-number version string like: "1.4", "1.5" which is
     * useful for for passing as '-source' argument for javac. If language level is not set,
     * the Java 9 is taken as default.
     * @return String 1.3, 1.4, 1.5, 1.6, 1.7, 1.8 or 9
     */
    @Override
    @Nullable
    public String getLanguageLevelAsNumber() {
        final String languageLevel = getLanguageLevel() != null ? getLanguageLevel() : DEFAULT_LANGUAGE_LEVEL;
        return languageLevel2VersionString.get(languageLevel);
    }
}
