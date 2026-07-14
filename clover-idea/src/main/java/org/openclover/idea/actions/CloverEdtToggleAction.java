package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;

/**
 * Base class for OpenClover's toggle actions whose {@code isSelected()}/{@code update()} must touch
 * Swing components directly and therefore genuinely need the Event Dispatch Thread. It is the EDT
 * counterpart of {@link CloverToggleAction}.
 */
public abstract class CloverEdtToggleAction extends CloverToggleAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
