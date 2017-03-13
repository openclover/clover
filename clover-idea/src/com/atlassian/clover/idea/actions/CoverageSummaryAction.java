package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.IProjectPlugin;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.report.jfc.PercentBarIcon;
import com.atlassian.clover.util.Formatting;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;

public class CoverageSummaryAction extends AnAction {

    public CoverageSummaryAction() {
        super("", "", null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        // this action is for display purposes only.
    }

    @Override
    public void update(AnActionEvent event) {

        Presentation presentation = event.getPresentation();
        IProjectPlugin plugin = ProjectPlugin.getPlugin(event);

        presentation.setVisible(plugin != null && plugin.getConfig().isShowSummaryInToolbar());

        final float newPc = plugin == null ? -1 : plugin.getCoverageManager().getCurrentCoverage();

        final PercentBarIcon myIcon = new PercentBarIcon(70, 14);
        myIcon.setPercent(newPc);
        presentation.setDisabledIcon(myIcon);

        presentation.setEnabled(false);
        final String msg = newPc < 0 ?
                "No Project Coverage Available" :
                "Project Coverage: " + Formatting.getPercentStr(newPc);

        presentation.setText(msg);
        presentation.setDescription(msg);
    }

}
