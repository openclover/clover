package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * Base class for OpenClover's action groups whose {@code update()} reads selection/PSI data that the
 * platform only supplies on the Event Dispatch Thread - for example a Project View context-menu group
 * whose visibility depends on {@code DataKeys.PSI_ELEMENT} / {@code DataKeys.MODULE_CONTEXT}.
 * It is the EDT counterpart of {@link BgtActionGroup}.
 */
public abstract class EdtActionGroup extends DefaultActionGroup {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
