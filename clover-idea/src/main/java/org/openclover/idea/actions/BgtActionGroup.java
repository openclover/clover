package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * Base class for OpenClover's action groups whose {@code update()} computes the group's
 * enabled/visible state (or presentation icon) purely from the action's {@code DataContext}
 * (project, PSI element, selection) and the OpenClover configuration/model - never from Swing
 * components directly.
 */
public abstract class BgtActionGroup extends DefaultActionGroup {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
