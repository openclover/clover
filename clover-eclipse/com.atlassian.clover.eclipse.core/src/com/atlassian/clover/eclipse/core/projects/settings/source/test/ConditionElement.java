package com.atlassian.clover.eclipse.core.projects.settings.source.test;

public abstract class ConditionElement implements TreeElement {
    private TreeElement parent;

    protected ConditionElement(TreeElement parent) {
        this.parent = parent;
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }
}
