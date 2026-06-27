package org.openclover.eclipse.core.views;

public abstract class BuiltinColumnDefinition extends ColumnDefinition {
    public BuiltinColumnDefinition(String id, int requiredIndex, int style, String title, String abbreviated, String tooltip) {
        super("Builtin:" + id, requiredIndex, style, title, abbreviated, tooltip);
    }

    @Override
    public boolean isCustom() {
        return false;
    }

}
