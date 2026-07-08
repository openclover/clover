package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;

import javax.swing.Icon;

/**
 * Base class for OpenClover's plain (stateless) actions - a single command that is triggered once
 * per invocation via {@code actionPerformed()}, e.g. "Refresh Coverage", "Delete", "Show Report Wizard".
 * <p>
 * Use this base for command-style actions. For actions that represent a persistent on/off state
 * (a checkbox/toggle whose selected state is read back from configuration), extend
 * {@link CloverToggleAction} instead.
 * <p>
 * All OpenClover actions compute their enabled/visible state in {@code update()} purely from the
 * action's {@code DataContext} (project, module, PSI element, selected coverage node) and the
 * OpenClover configuration/model - never from Swing components directly - so it is safe (and, since
 * IDEA 2022.3, required to be declared) to run {@code update()} on a background thread. This class
 * centralizes that declaration by returning {@link ActionUpdateThread#BGT}; a subclass that genuinely
 * needs to touch Swing state in {@code update()} must override this to return
 * {@link ActionUpdateThread#EDT}.
 */
public abstract class CloverAnAction extends AnAction {

    protected CloverAnAction() {
        super();
    }

    protected CloverAnAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
