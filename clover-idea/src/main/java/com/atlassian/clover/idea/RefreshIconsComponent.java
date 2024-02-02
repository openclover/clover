package com.atlassian.clover.idea;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.config.ConfigChangeEvent;
import com.atlassian.clover.idea.config.ConfigChangeListener;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class RefreshIconsComponent implements ProjectComponent {
    private final Project project;

    public RefreshIconsComponent(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(project);
        if (plugin != null) {
            plugin.getConfig().addConfigChangeListener(evt -> {
                if (evt.hasPropertyChange(IdeaCloverConfig.VIEW_INCLUDE_ANNOTATION)
                        || evt.hasPropertyChange(IdeaCloverConfig.ENABLED)
                        || evt.hasPropertyChange(IdeaCloverConfig.INCLUDES)
                        || evt.hasPropertyChange(IdeaCloverConfig.EXCLUDES)
                        || evt.hasPropertyChange(IdeaCloverConfig.INSTRUMENT_TESTS)) {

                    ApplicationManager.getApplication().runWriteAction(() -> ProjectView.getInstance(project).refresh());
                }
            });
        }
    }

    @Override
    public void projectClosed() {
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "CloverRefreshIconsComponent";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }
}
