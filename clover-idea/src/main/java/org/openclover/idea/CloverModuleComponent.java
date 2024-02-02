package com.atlassian.clover.idea;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.config.CloverModuleConfig;
import com.atlassian.clover.idea.config.IdeaXmlConfigConstants;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.build.ProjectRebuilder;
import com.atlassian.clover.idea.config.CloverModuleConfigPanel;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.BorderFactory;

@State(name = IdeaXmlConfigConstants.MODULE_FILE_COMPONENT_NAME, storages = {@Storage(id = "module", file = "$MODULE_FILE$")})
public class CloverModuleComponent implements ModuleComponent, Configurable,
        PersistentStateComponent<CloverModuleConfig> {

    private CloverModuleConfig config = new CloverModuleConfig();

    private CloverModuleConfigPanel configComponent;

    private final Project project;

    public CloverModuleComponent(Project project) {
        this.project = project;
    }

    @Nullable
    public static CloverModuleComponent getInstance(Module module) {
        return module == null ? null : module.getComponent(CloverModuleComponent.class);
    }

    public CloverModuleConfig getConfig() {
        return config;
    }


    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void moduleAdded() {
    }

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "Clover";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @Override
    @Nls
    public String getDisplayName() {
        return "Clover";
    }

    @Override
    @Nullable
    @NonNls
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(project);
        if (plugin != null && plugin.isEnabled()) {
            configComponent = new CloverModuleConfigPanel();
            configComponent.setConfig(config);
            return configComponent;
        } else {
            final JLabel msg =  new JLabel("Clover plugin is disabled for current project");
            msg.setHorizontalAlignment(JLabel.CENTER);
            msg.setVerticalAlignment(JLabel.TOP);
            msg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            return msg;
        }
    }

    @Override
    public boolean isModified() {
        return configComponent != null && !config.equals(configComponent.getConfig());
    }

    @Override
    public void apply() throws ConfigurationException {
        if (configComponent == null) {
            return;
        }
        final CloverModuleConfig newConfig = configComponent.getConfig();
        final boolean changed = !newConfig.equals(config);
        config = newConfig;
        if (changed) {
            ProjectRebuilder.getInstance(project).rebuildProject(newConfig.isExcluded()); // db cleanup necessary only when excluding sth
        }
    }

    @Override
    public void reset() {
        if (configComponent != null) {
            configComponent.setConfig(config);
        }
    }

    @Override
    public void disposeUIResources() {
        configComponent = null;
    }

    @Override
    public CloverModuleConfig getState() {
        return config;
    }

    @Override
    public void loadState(CloverModuleConfig state) {
        config = new CloverModuleConfig(state);
    }
}

