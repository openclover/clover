package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.ToggleAction;

/**
 * Base class for OpenClover's toggle actions - actions that carry a persistent on/off state rendered
 * as a checkbox or a pressed toolbar button, e.g. "Toggle Build with OpenClover", "Autoscroll to
 * Source", "Flatten Packages". Unlike {@link CloverAnAction} (a one-shot command), a toggle action
 * reports its current state through {@code isSelected()} and applies a new state through
 * {@code setSelected()} rather than doing its work in {@code actionPerformed()}.
 * <p>
 * As with {@link CloverAnAction}, the state is derived only from the action's {@code DataContext} and
 * the OpenClover configuration/model (not from Swing components), so {@code update()}/{@code isSelected()}
 * can run on a background thread. This class centralizes the IDEA 2022.3+ requirement by returning
 * {@link ActionUpdateThread#BGT}; a subclass that must touch Swing state should override this to return
 * {@link ActionUpdateThread#EDT}.
 */
public abstract class CloverToggleAction extends ToggleAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
