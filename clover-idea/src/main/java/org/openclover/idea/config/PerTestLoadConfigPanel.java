package org.openclover.idea.config;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.GridBagLayout;

public class PerTestLoadConfigPanel extends ConfigPanel {
    private JCheckBox loadPerTestCvg = new JCheckBox("<html>Load per-test coverage data (may be slow for big projects)");

    public PerTestLoadConfigPanel() {
        initLayout();
    }

    private void initLayout() {
        setBorder(getEnabledBorder());

        setLayout(new GridBagLayout());

        add(loadPerTestCvg, new GBC(1, 1).setInsets(0, 6, 5, 6).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0));
        add(new JPanel(), new GBC(1, 2).setFill(GBC.HORIZONTAL).setWeight(1.0, 1.0));

    }

    @Override
    public String getTitle() {
        return "Per-test Coverage Data";
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        config.setLoadPerTestData(loadPerTestCvg.isSelected());
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        loadPerTestCvg.setSelected(config.isLoadPerTestData());
    }

    public static void main(String[] argv) {
        ConfigPanelRunner.run(new PerTestLoadConfigPanel());
    }
}