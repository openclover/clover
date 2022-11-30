package com.atlassian.clover.idea.util.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

import static clover.com.google.common.collect.Maps.newHashMap;

public class BorderLayoutConverter extends BorderLayout {

    private final Map<Component, String> storedComponentMap = newHashMap();
    private final Component layoutComponent;

    private boolean freeze;
    private boolean doFlip;

    public BorderLayoutConverter(Component component) {
        this.layoutComponent = component;
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                final Dimension dimension = e.getComponent().getSize();
                setFlip(dimension.getWidth() > dimension.getHeight() * 1.25);
            }
        });
    }

    @Deprecated
    @Override
    public void addLayoutComponent(String name, Component comp) {
        if (!freeze) {
            storedComponentMap.put(comp, name);
        }
        super.addLayoutComponent(name, comp);
    }

    private static final Map<String, String> TRANSLATION = new HashMap<String, String>() {
        {
            put(NORTH, WEST);
            put(WEST, NORTH);
            put(CENTER, CENTER);
            put(SOUTH, EAST);
            put(EAST, SOUTH);
        }
    };

    public void setFlip(boolean flip) {
        if (flip == doFlip) {
            return;
        }
        freeze = true; // no more recording of components
        doFlip = flip;
        for (Map.Entry<Component, String> entry : storedComponentMap.entrySet()) {
            final Component component = entry.getKey();
            final String constraint = flip ? TRANSLATION.get(entry.getValue()) : entry.getValue();

            removeLayoutComponent(component);
            addLayoutComponent(component, constraint);
        }
        layoutComponent.invalidate();
    }
}

