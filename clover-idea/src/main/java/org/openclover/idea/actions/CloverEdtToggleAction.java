package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;

/**
 * Base class for OpenClover's toggle actions whose {@code isSelected()}/{@code update()} read
 * selection/PSI data that the platform only supplies on the Event Dispatch Thread - for example a
 * Project View context-menu toggle whose state and label depend on {@code DataKeys.PSI_ELEMENT}.
 * It is the EDT counterpart of {@link CloverToggleAction}.
 */
public abstract class CloverEdtToggleAction extends CloverToggleAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
