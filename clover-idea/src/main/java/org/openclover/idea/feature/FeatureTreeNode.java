package com.atlassian.clover.idea.feature;

import java.util.Collections;
import java.util.List;

import static org.openclover.util.Lists.newArrayList;

public class FeatureTreeNode implements CategoryListener {

    private final Category category;
    private final List<FeatureTreeNode> children = newArrayList();

    private FeatureTreeNode parent = null;

    private boolean featureEnabled = false;

    private final List<FeatureListener> listeners = newArrayList();

    /**
     *
     * @param c
     */
    public FeatureTreeNode(Category c) {
        category = c;
        category.addCategoryListener(this);
        featureEnabled = isFeatureEnabled();
    }

    public void addChild(FeatureTreeNode n) {
        if (!children.contains(n)) {
            children.add(n);
            n.setParent(this);
        }
    }

    public List<FeatureTreeNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addFeatureListener(FeatureListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeFeatureListener(FeatureListener l) {
        listeners.remove(l);
    }

    private void setParent(FeatureTreeNode n) {
        parent = n;
        featureEnabled = isFeatureEnabled();
    }

    @Override
    public void categoryStateChanged(CategoryEvent evt) {
        update(evt.getSource());
    }

    public String getCategoryName() {
        return category.getName();
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        String name = getCategoryName();
        if (parent != null) {
            name = parent.getName() + "-" + name;
        }
        return name;
    }

    protected void update(Object source) {
        if (featureEnabled == isFeatureEnabled()) {
            return;
        }
        featureEnabled = !featureEnabled;
//        featureEnabled = isFeatureEnabled();

        // generate feature changed event.
        FeatureEvent evt = new FeatureEvent(source, featureEnabled, getName());
        for (FeatureListener listener : newArrayList(listeners)) {
            listener.featureStateChanged(evt);
        }

        // update children.
        for (FeatureTreeNode child : children) {
            child.update(source);
        }
    }

    public boolean isFeatureEnabled() {
        if (category.isEnabled()) {
            if (parent != null) {
                return parent.isFeatureEnabled();
            }
            return true;
        }
        return false;
    }
}
