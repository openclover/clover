package org.openclover.idea.actions.excludes;

import org.openclover.idea.config.IdeaCloverConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import org.jetbrains.annotations.NotNull;

public class ToggleExcludeAction extends AbstractToggleInclusionAction {
    @Override
    String getExistingPattern(@NotNull IdeaCloverConfig config) {
        return config.getExcludes();
    }

    @Override
    void setPattern(@NotNull IdeaCloverConfig config, String pattern) {
        config.setExcludes(pattern);
        config.notifyListeners();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        final String pattern = getPattern(e.getData(DataKeys.PSI_ELEMENT));
        if (pattern == null) {
            e.getPresentation().setText("Invalid element: " + e.getData(DataKeys.PSI_ELEMENT));
            e.getPresentation().setEnabled(false);
        } else {
            String name = ExclusionUtil.getDisplayName(pattern);
            e.getPresentation().setText("Exclude " + name);
            e.getPresentation().setEnabled(true);
        }
    }
}
