package org.openclover.idea.actions.excludes;

import com.intellij.ide.projectView.ProjectView;
import org.openclover.idea.actions.CloverAnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import org.openclover.idea.CloverModuleComponent;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.CloverModuleConfig;

public class ToggleModuleExclusionAction extends CloverAnAction {
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
        final Module module = e.getData(DataKeys.MODULE_CONTEXT);
        final CloverModuleComponent component = CloverModuleComponent.getInstance(module);
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        final boolean visible = component != null && plugin != null && plugin.isEnabled();
        e.getPresentation().setVisible(visible);
        if (component != null) {
            e.getPresentation().setText(component.getConfig().isExcluded() ?
                    "Include module in OpenClover instrumentation" :
                    "Exclude module from OpenClover instrumentation");
        }
    }
}
