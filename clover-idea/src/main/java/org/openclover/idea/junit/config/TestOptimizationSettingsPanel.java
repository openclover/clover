package org.openclover.idea.junit.config;

import com.intellij.ui.components.panels.VerticalBox;
import org.openclover.idea.config.GBC;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.GridBagLayout;

public class TestOptimizationSettingsPanel extends JPanel {
    private final JCheckBox displayDialog = new JCheckBox("Display a dialog");
    private final JCheckBox displayBalloon = new JCheckBox("Display a balloon");

    public TestOptimizationSettingsPanel() {
        setLayout(new GridBagLayout());

        final VerticalBox noTests = new VerticalBox();
        noTests.setBorder(BorderFactory.createTitledBorder("When Test Optimization decides no tests need to be run"));
        noTests.add(displayDialog);
        noTests.add(displayBalloon);
        add(noTests, new GBC(0, 0).setWeight(1, 0).setFill(GBC.BOTH));

        add(new JPanel(), new GBC(0, 1).setWeight(1, 1).setFill(GBC.BOTH));
    }

    public JCheckBox getDisplayDialog() {
        return displayDialog;
    }

    public JCheckBox getDisplayBalloon() {
        return displayBalloon;
    }
}
