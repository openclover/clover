package org.openclover.idea.actions.excludes;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.config.IdeaCloverConfig;

public class ToggleIncludeAction extends AbstractToggleInclusionAction {
    @Override
    String getExistingPattern(@NotNull IdeaCloverConfig config) {
        return config.getIncludes();
    }

    @Override
    void setPattern(@NotNull IdeaCloverConfig config, String pattern) {
        config.setIncludes(pattern);
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
            e.getPresentation().setText("Include " + name);
            e.getPresentation().setEnabled(true);
        }
    }
}
