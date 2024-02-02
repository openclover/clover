package org.openclover.idea.actions;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.util.ModelScope;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ModelScopeAction extends ComboBoxAction {

    private final DefaultActionGroup actionGroup;

    public ModelScopeAction() {
        this.actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("CloverPlugin.Scope");
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent jComponent) {
        return actionGroup;
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.add(new JLabel(" Show:"));

        ComboBoxButton button = new ComboBoxButton(presentation);
        Insets margins = button.getMargin();
        //noinspection SuspiciousNameCombination
        margins.left = margins.top;
        button.setMargin(margins);

        // Constraints from IDEA's CallHierarchyBrowser
        jPanel.add(button,
                   new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 3, 0, 3), 0, 0));
        return jPanel;
    }

    @Override
    public void update(AnActionEvent event) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(event);
        final ModelScope scope = projectPlugin != null ?
                projectPlugin.getConfig().getModelScope() :
                ModelScope.ALL_CLASSES;

        event.getPresentation().setText(AbstractClassesScopeAction.getText(scope));
    }
}
