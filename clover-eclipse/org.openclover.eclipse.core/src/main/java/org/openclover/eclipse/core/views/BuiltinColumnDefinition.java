package com.atlassian.clover.eclipse.core.views;

import com.atlassian.clover.reporters.Column;

public abstract class BuiltinColumnDefinition extends ColumnDefinition {
    private Column columnPrototype;

    public BuiltinColumnDefinition(String id, int requiredIndex, int style, String title, String abbreviated, String tooltip) {
        super("Builtin:" + id, requiredIndex, style, title, abbreviated, tooltip);
    }

    @Override
    public boolean isCustom() {
        return false;
    }

}
