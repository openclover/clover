package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.feature.CloverFeatures;

public class ToggleInstrumentationAction extends ToggleAction {

    @Override
    public boolean isSelected(final AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        return project != null && ProjectPlugin.getPlugin(project).getFeatureManager().isFeatureEnabled(CloverFeatures.CLOVER_BUILDING);
    }

    @Override
    public void setSelected(final AnActionEvent event, boolean selected) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            ProjectPlugin.getPlugin(project).getFeatureManager().setCategoryEnabled(CloverFeatures.CLOVER_BUILDING, selected);
        }
    }
}
