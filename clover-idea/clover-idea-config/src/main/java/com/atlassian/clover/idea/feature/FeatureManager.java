package com.atlassian.clover.idea.feature;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import static clover.com.google.common.collect.Lists.newArrayList;

public class FeatureManager implements FeatureListener {

    private final List<FeatureListener> listeners = newArrayList();

    private final List<FeatureTreeNode> featureTrees = newArrayList();

    public void addFeatureListener(FeatureListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void addFeatureListener(String featurePath, FeatureListener l) {
        FeatureTreeNode node = lookup(featurePath);
        if (node == null) {
            throw new IllegalArgumentException("Requested feature " + featurePath + " was not found");
        }
        node.addFeatureListener(l);
    }

    public void removeFeatureListener(String featurePath, FeatureListener l) {
        FeatureTreeNode node = lookup(featurePath);
        if (node == null) {
            throw new IllegalArgumentException("Requested feature " + featurePath + " was not found");
        }
        node.removeFeatureListener(l);
    }

    public void removeFeatureListener(FeatureListener l) {
        listeners.remove(l);
    }

    public void registerFeatureTree(FeatureTreeNode rootNode) {
        if (!featureTrees.contains(rootNode)) {
            featureTrees.add(rootNode);
            listenTo(rootNode);
        }
    }

    public boolean isFeatureEnabled(String featurePath) {
        FeatureTreeNode node = lookup(featurePath);
        return node != null && node.isFeatureEnabled();
    }

    public void setCategoryEnabled(String categoryPath, boolean b) {
        FeatureTreeNode node = lookup(categoryPath);
        if (node != null) {
            node.getCategory().setEnabled(b);
        }
    }

    private void listenTo(FeatureTreeNode node) {
        node.addFeatureListener(this);
        for (FeatureTreeNode child : node.getChildren()) {
            listenTo(child);
        }
    }

    @Override
    public void featureStateChanged(FeatureEvent evt) {
        // pass on to listeners.
        for (FeatureListener listener : newArrayList(listeners)) {
            listener.featureStateChanged(evt);
        }
    }

    public FeatureTreeNode lookup(String featurePath) {

        StringTokenizer tokens = new StringTokenizer(featurePath, "-", false);
        FeatureTreeNode current = null;
        Iterator nodes = featureTrees.iterator();
        while (tokens.hasMoreTokens()) {
            String name = tokens.nextToken();
            boolean found = false;
            while (nodes.hasNext() && !found) {
                FeatureTreeNode node = (FeatureTreeNode) nodes.next();
                if (node.getCategoryName().compareTo(name) == 0) {
                    found = true;
                    current = node;
                }
            }
            if (!found) {
                return null;
            }
            nodes = current.getChildren().iterator();
        }
        return current;
    }
}
