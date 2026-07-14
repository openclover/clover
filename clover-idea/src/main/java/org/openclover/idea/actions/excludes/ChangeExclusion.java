package org.openclover.idea.actions.excludes;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.psi.PsiElement;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.actions.BgtActionGroup;

public class ChangeExclusion extends BgtActionGroup {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        final PsiElement psiElement = e.getData(DataKeys.PSI_ELEMENT);
        final boolean configEnabled = plugin != null && plugin.getConfig().isEnabled();
        final boolean exclusionEnabled = ExclusionUtil.isEnabled(psiElement, e.getData(DataKeys.PROJECT));

        // A PsiDirectory outside any source root (e.g. a module/project root) has no package, so
        // AbstractToggleInclusionAction.getPattern() can never build an include/exclude glob for it -
        // the Include/Exclude children would just show "Invalid element: ...". Hide the whole group
        // rather than show entries that can't do anything.
        final boolean hasPattern = AbstractToggleInclusionAction.getPattern(psiElement) != null;
        final boolean visible = plugin != null && configEnabled && exclusionEnabled && hasPattern;
        e.getPresentation().setVisible(visible);
    }
}
