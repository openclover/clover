package org.openclover.idea.actions.excludes;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class ChangeExclusion extends DefaultActionGroup {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        e.getPresentation().setVisible(plugin != null
                && plugin.getConfig().isEnabled()
                && ExclusionUtil.isEnabled(e.getData(DataKeys.PSI_ELEMENT), e.getData(DataKeys.PROJECT)));
    }


}
