package com.atlassian.clover.idea.build;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;

public class RebuildProjectDialog extends DialogWrapper {
    private static final String TITLE = "Project rebuild required";
    private static final String TITLE_CLEAN = "Project rebuild with Clover database cleaned required";

    private static final String DESCRIPTION = "<html>Clover settings change requires project rebuild.";
    private static final String DESCRIPTION_CLEAN = "<html>Clover settings change requires Clover database cleanup and project rebuild.";

    private final String description;

    private final JCheckBox askNextTime = new JCheckBox("Prompt me again when rebuild is required", true);

    public RebuildProjectDialog(Project project, boolean cleanDb) {
        super(project, false);

        description = cleanDb ? DESCRIPTION_CLEAN : DESCRIPTION;
        setTitle(cleanDb ? TITLE_CLEAN : TITLE);
        setOKButtonText(cleanDb ? "Clean and rebuild now" : "Rebuild now");
        setSkipButtonText("Skip rebuild");

        init();
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), skipAction};
    }

    private void setSkipButtonText(String text) {
        skipAction.putValue(Action.NAME, text);
    }

    private final AbstractAction skipAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            close(CANCEL_EXIT_CODE);
        }
    };


    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        FormLayout formLayout = new FormLayout("3dlu, pref, 3dlu, 15dlu, pref:g, 3dlu",
                                               "3dlu, pref, 9dlu, pref:g, 3dlu");
        CellConstraints iconCC = new CellConstraints(2, 2, 1, 3);
        CellConstraints descrCC = new CellConstraints(4, 2, 2, 1);
        CellConstraints choiceCC = new CellConstraints(5, 4, 1, 1);

        JPanel thePanel = new JPanel(formLayout);

        JLabel iconLabel = new JLabel(Messages.getWarningIcon());
        iconLabel.setVerticalAlignment(JLabel.TOP);
        thePanel.add(iconLabel, iconCC);

        thePanel.add(new JLabel(description), descrCC);
        thePanel.add(askNextTime, choiceCC);

        return thePanel;
    }

    public boolean rebuildRequested() {
        return getExitCode() == OK_EXIT_CODE;
    }

    public boolean askNextTime() {
        return askNextTime.isSelected();
    }
}
