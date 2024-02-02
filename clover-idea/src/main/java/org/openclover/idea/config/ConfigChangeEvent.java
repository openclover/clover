package org.openclover.idea.config;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

import static org.openclover.util.Maps.newHashMap;

/**
 * The Config Change Event encapsulates a number of property changes. Consider
 * it a batch of property change events.
 */
public class ConfigChangeEvent extends EventObject {

    private final Map<String, PropertyChangeEvent> changedProperties = newHashMap();

    public ConfigChangeEvent(Object source) {
        this(source, Collections.<PropertyChangeEvent>emptyList());
    }

    public ConfigChangeEvent(Object source, List<PropertyChangeEvent> propertyChangeEvents) {
        super(source);
        for (PropertyChangeEvent evt : propertyChangeEvents) {
            changedProperties.put(evt.getPropertyName(), evt);
        }
    }

    /**
     * @param propertyName String
     * @return boolean
     */
    public boolean hasPropertyChange(String propertyName) {
        return changedProperties.containsKey(propertyName);
    }

    /**
     * @param propertyName String
     * @return PropertyChangeEvent
     */
    public PropertyChangeEvent getPropertyChange(String propertyName) {
        return changedProperties.get(propertyName);
    }

    /**
     *
     * @return
     */
    public PropertyChangeEvent[] getPropertyChanges() {
        Collection<PropertyChangeEvent> properties = changedProperties.values();
        return properties.toArray(new PropertyChangeEvent[properties.size()]);
    }
}
