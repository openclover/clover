package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;

import javax.swing.Icon;

/**
 * Base class for OpenClover actions whose {@code update()} needs the Event Dispatch Thread-bound
 * (example - coverage model, mutates the coverage tree). It is the EDT counterpart of {@link CloverAnAction}.
 */
public abstract class CloverEdtAction extends AnAction {

    protected CloverEdtAction() {
        super();
    }

    protected CloverEdtAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
