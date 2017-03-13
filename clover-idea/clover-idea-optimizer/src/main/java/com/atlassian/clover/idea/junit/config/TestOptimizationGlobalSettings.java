package com.atlassian.clover.idea.junit.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.OptionalConfigurable;
import org.jetbrains.annotations.Nls;

import javax.swing.Icon;
import javax.swing.JComponent;

@State(name = "CloverTestOptimization", storages = {@Storage(id = "other", file = "$APP_CONFIG$/other.xml")})
public class TestOptimizationGlobalSettings implements PersistentStateComponent<TestOptimizationGlobalSettings>,
        OptionalConfigurable, Configurable {
    private boolean showAllTestsOptimizedOutDialog = true;
    private boolean showAllTestsOptimizedOutBalloon = true;

    public static TestOptimizationGlobalSettings getInstance() {
        return ServiceManager.getService(TestOptimizationGlobalSettings.class);
    }

    @Override
    @Nls
    public String getDisplayName() {
        return "Test Optimization";
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    private TestOptimizationSettingsPanel configPanel;

    @Override
    public JComponent createComponent() {
        if (configPanel == null) {
            configPanel = new TestOptimizationSettingsPanel();
        }
        return configPanel;
    }

    @Override
    public void apply() throws ConfigurationException {
        showAllTestsOptimizedOutDialog = configPanel.getDisplayDialog().isSelected();
        showAllTestsOptimizedOutBalloon = configPanel.getDisplayBalloon().isSelected();
    }

    @Override
    public void reset() {
        configPanel.getDisplayDialog().setSelected(showAllTestsOptimizedOutDialog);
        configPanel.getDisplayBalloon().setSelected(showAllTestsOptimizedOutBalloon);
    }

    @Override
    public boolean isModified() {
        return showAllTestsOptimizedOutDialog != configPanel.getDisplayDialog().isSelected() ||
                showAllTestsOptimizedOutBalloon != configPanel.getDisplayBalloon().isSelected();

    }

    @Override
    public void disposeUIResources() {
        configPanel = null;
    }

    @Override
    public boolean needDisplay() {
        return false;
    }

    @Override
    public TestOptimizationGlobalSettings getState() {
        return this;
    }

    @Override
    public void loadState(TestOptimizationGlobalSettings settings) {
        showAllTestsOptimizedOutBalloon = settings.showAllTestsOptimizedOutBalloon;
        showAllTestsOptimizedOutDialog = settings.showAllTestsOptimizedOutDialog;
    }

    public boolean isShowAllTestsOptimizedOutDialog() {
        return showAllTestsOptimizedOutDialog;
    }

    public void setShowAllTestsOptimizedOutDialog(boolean showAllTestsOptimizedOutDialog) {
        this.showAllTestsOptimizedOutDialog = showAllTestsOptimizedOutDialog;
    }

    public boolean isShowAllTestsOptimizedOutBalloon() {
        return showAllTestsOptimizedOutBalloon;
    }

    public void setShowAllTestsOptimizedOutBalloon(boolean showAllTestsOptimizedOutBalloon) {
        this.showAllTestsOptimizedOutBalloon = showAllTestsOptimizedOutBalloon;
    }

}
