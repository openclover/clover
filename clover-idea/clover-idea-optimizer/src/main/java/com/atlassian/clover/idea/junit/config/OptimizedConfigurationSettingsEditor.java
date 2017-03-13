package com.atlassian.clover.idea.junit.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class OptimizedConfigurationSettingsEditor extends SettingsEditor<OptimizedConfigurationSettings> {
    private OptimizedConfigurationSettingsPanel editor;

    @Override
    protected void resetEditorFrom(OptimizedConfigurationSettings s) {
        editor.resetEditorFrom(s);
    }

    @Override
    protected void applyEditorTo(OptimizedConfigurationSettings s) throws ConfigurationException {
        editor.applyEditorTo(s);
    }

    @Override
    @NotNull
    protected JComponent createEditor() {
        if (editor == null) {
            editor = new OptimizedConfigurationSettingsPanel();
        }
        return editor;
    }

    @Override
    protected void disposeEditor() {
        editor = null;
    }
}
