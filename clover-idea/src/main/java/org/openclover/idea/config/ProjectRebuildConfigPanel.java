package org.openclover.idea.config;

import com.intellij.openapi.ui.VerticalFlowLayout;
import org.openclover.idea.util.ui.RichLabel;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

public class ProjectRebuildConfigPanel extends ConfigPanel {
    private static final String TITLE = "Rebuild policy";
    private static final String DESCRIPTION =
            "<html>Clover requires that the project is rebuilt every time the <i>Build with Clover</i> is toggled or " +
                    "coverage database is purged.";
    private final JLabel description = new RichLabel(DESCRIPTION);

    private final JRadioButton askButton = new JRadioButton("Ask");
    private final JRadioButton alwaysButton = new JRadioButton("Rebuild immediately");
    private final JRadioButton neverButton = new JRadioButton("Never rebuild automatically");

    private final ButtonGroup buttonGroup = new ButtonGroup();

    public ProjectRebuildConfigPanel() {
        buttonGroup.add(askButton);
        buttonGroup.add(alwaysButton);
        buttonGroup.add(neverButton);

        setLayout(new VerticalFlowLayout());
        add(description);
        add(askButton);
        add(alwaysButton);
        add(neverButton);
    }

    private ProjectRebuild getSelection() {
        return neverButton.isSelected() ? ProjectRebuild.NEVER :
                alwaysButton.isSelected() ? ProjectRebuild.ALWAYS : ProjectRebuild.ASK;
    }

    @Override
    public void commitTo(CloverPluginConfig data) {
        data.setProjectRebuild(getSelection());
    }

    @Override
    public void loadFrom(CloverPluginConfig data) {
        ProjectRebuild rebuild = data.getProjectRebuild();
        switch (rebuild) {
            case ASK:
                askButton.setSelected(true);
                break;
            case ALWAYS:
                alwaysButton.setSelected(true);
                break;
            case NEVER:
                neverButton.setSelected(true);
                break;
        }
    }

    @Override
    public String getTitle() {
        return TITLE;
    }
}
