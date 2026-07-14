package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * Base class for OpenClover's action groups whose {@code update()} must touch Swing components
 * directly (not just presentation/config/PSI data) and therefore genuinely needs the Event Dispatch
 * Thread. It is the EDT counterpart of {@link BgtActionGroup}.
 */
public abstract class EdtActionGroup extends DefaultActionGroup {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
