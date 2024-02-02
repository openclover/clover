package org.openclover.idea.actions.excludes;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractToggleInclusionAction extends ToggleAction {

    abstract String getExistingPattern(IdeaCloverConfig config);
    abstract void setPattern(IdeaCloverConfig config, String pattern);

    @Nullable
    static String getPattern(PsiElement element) {
        final String recursivePattern = ExclusionUtil.getRecursivePattern(element);
        return recursivePattern != null ? recursivePattern : ExclusionUtil.getPattern(element);
    }

    static IdeaCloverConfig getConfig(AnActionEvent e) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(e);
        return plugin != null ? plugin.getConfig() : null;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        final String pattern = getPattern(e.getData(DataKeys.PSI_ELEMENT));
        final IdeaCloverConfig config = getConfig(e);
        return pattern != null && config != null && ExclusionUtil.isExplicitlyIncluded(getExistingPattern(config), pattern);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        final String pattern = getPattern(e.getData(DataKeys.PSI_ELEMENT));
        final IdeaCloverConfig config = getConfig(e);
        if (config == null) {
            return;
        }
        final String existingPatterns = getExistingPattern(config);
        if (!ExclusionUtil.isExplicitlyIncluded(existingPatterns, pattern)) {
            if (existingPatterns == null || existingPatterns.trim().length() == 0) {
                setPattern(config, pattern);
            } else {
                setPattern(config, existingPatterns + ", " + pattern);
            }
        } else {
            final String newPattern = ExclusionUtil.removePattern(existingPatterns, pattern);
            if (!newPattern.equals(pattern)) {
                setPattern(config, newPattern);
            }
        }
    }
}
