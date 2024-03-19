package org.openclover.idea.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.openclover.idea.ProjectPlugin;

import javax.swing.JComponent;

public class CloverProjectConfigurable implements Configurable {

    public static final String DISPLAY_NAME = "OpenClover (project settings)";
    private final IdeaCloverConfig config;
    private final Project project;
    private ProjectConfigPanel configPanel;

    public CloverProjectConfigurable(Project project) {
        this.project = project;
        // get a direct reference to config stored in a ProjectPlugin, we'll be updating it
        config = ProjectPlugin.getPlugin(project).getConfig();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    /**
     * Get the configuration panel, used to present the user with an interface through which the configuration can be
     * updated.
     */
    @Override
    public JComponent createComponent() {
        if (configPanel == null) {
            configPanel = new ProjectConfigPanel(project, config);
        }
        return configPanel;
    }

    /**
     * Return true if the configuration panel has changes that have not yet been applied(saved).
     */
    @Override
    public boolean isModified() {
        IdeaCloverConfig tmpConfig = IdeaCloverConfig.fromProject(project);
        tmpConfig = config.copyConfigTo(tmpConfig);
        tmpConfig.markDirty(false);
        configPanel.commitTo(tmpConfig);
        return tmpConfig.isDirty();
    }

    /**
     * Apply the changes held by the configuration panel to the internal configuration.
     */
    @Override
    public void apply() throws ConfigurationException {
        configPanel.commitTo(config);
    }

    /**
     * Reset the state of the configuration panel, clearing any changes that have not yet been applied.
     */
    @Override
    public void reset() {
        configPanel.loadFrom(config);
    }

    @Override
    public void disposeUIResources() {
        if (configPanel != null) {
            configPanel = null;
        }
    }

}
