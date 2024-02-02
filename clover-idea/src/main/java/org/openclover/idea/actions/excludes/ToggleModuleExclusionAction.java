package org.openclover.idea.actions.excludes;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.config.CloverModuleConfig;
import org.openclover.idea.CloverModuleComponent;
import org.openclover.idea.ProjectPlugin;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;

public class ToggleModuleExclusionAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final Module module = e.getData(DataKeys.MODULE_CONTEXT);
        final CloverModuleComponent component = CloverModuleComponent.getInstance(module);
        if (module != null && component != null) {
            final CloverModuleConfig moduleConfig = component.getConfig();
            moduleConfig.setExcluded(!moduleConfig.isExcluded());
            ProjectView.getInstance(module.getProject()).refresh();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final CloverModuleComponent component = CloverModuleComponent.getInstance(e.getData(DataKeys.MODULE_CONTEXT));
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        e.getPresentation().setVisible(component != null && plugin != null && plugin.isEnabled());
        if (component != null) {
            e.getPresentation().setText(component.getConfig().isExcluded() ?
                    "Include module in Clover instrumentation" :
                    "Exclude module from Clover instrumentation");
        }
    }
}
