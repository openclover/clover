package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.report.jfc.PercentBarIcon;
import org.openclover.runtime.util.Formatting;

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
