package com.atlassian.clover.idea.feature;

import java.util.EventObject;

public class FeatureEvent extends EventObject {

    private final boolean enabled;
    private final String name;

    public FeatureEvent(Object source, boolean featureEnabled, String featureName) {
        super(source);
        enabled = featureEnabled;
        name = featureName;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof FeatureEvent)) {
            return false;
        }
        FeatureEvent otherEvent = (FeatureEvent) other;
        return otherEvent.enabled == enabled &&
                otherEvent.name.compareTo(name) == 0 &&
                otherEvent.getSource() == getSource();
    }
}
