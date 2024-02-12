package org.openclover.idea.actions.testviewscope;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.TestViewScope;

import javax.swing.JComponent;
import java.util.EnumMap;
import java.util.Map;

public class TestViewScopeAction extends ComboBoxAction {
    private final Map<TestViewScope, String> index = new EnumMap<>(TestViewScope.class);
    private final DefaultActionGroup actionGroup;


    public TestViewScopeAction() {
        actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("CloverPlugin.TestCaseScopeCombo");

        for (AnAction action : actionGroup.getChildren(null)) {
            if (action instanceof AbstractTestViewScopeAction) {
                AbstractTestViewScopeAction a = (AbstractTestViewScopeAction) action;
                index.put(a.getActionType(), a.getTemplatePresentation().getText());
            }
        }
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent button) {
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        final IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(e);
        final TestViewScope scope = projectPlugin != null ?
                projectPlugin.getConfig().getTestViewScope() :
                TestViewScope.GLOBAL;

        e.getPresentation().setText(index.get(scope));

    }
}
