package com.atlassian.clover.idea.feature;

import java.util.EventObject;

public class CategoryEvent extends EventObject {

    private final boolean enabled;
    private final String name;

    public CategoryEvent(Object source, String categoryName, boolean categoryEnabled) {
        super(source);
        name = categoryName;
        enabled = categoryEnabled;
    }

    public String getCategoryName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
