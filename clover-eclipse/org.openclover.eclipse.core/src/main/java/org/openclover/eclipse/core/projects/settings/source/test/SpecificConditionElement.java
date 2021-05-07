package org.openclover.eclipse.core.projects.settings.source.test;

public abstract class SpecificConditionElement extends ConditionElement {
    private String value;

    protected SpecificConditionElement(TreeElement parent, String value) {
        super(parent);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
