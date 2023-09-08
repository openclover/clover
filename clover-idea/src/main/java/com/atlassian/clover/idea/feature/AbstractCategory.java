package com.atlassian.clover.idea.feature;

import java.util.List;

import static org.openclover.util.Lists.newArrayList;

public abstract class AbstractCategory implements Category {

    private final String name;

    public AbstractCategory(String categoryName) {
        name = categoryName;
    }

    private final List<CategoryListener> listeners = newArrayList();

    @Override
    public void addCategoryListener(CategoryListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Override
    public void removeCategoryListener(CategoryListener l) {
        listeners.remove(l);
    }

    protected void fireCategoryEvent(CategoryEvent evt) {
        for (CategoryListener l : listeners) {
            l.categoryStateChanged(evt);
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
