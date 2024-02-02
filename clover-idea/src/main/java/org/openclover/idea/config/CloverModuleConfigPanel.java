package org.openclover.idea.config;

import org.openclover.idea.config.CloverModuleConfig;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * Configuration panel for Clover for a single project module.
 */
public class CloverModuleConfigPanel extends JPanel {
    private final JCheckBox excludedCheckBox = new JCheckBox("Exclude entire module from Clover instrumentation");

    public CloverModuleConfigPanel() {
        add(excludedCheckBox);
    }

    public void setConfig(CloverModuleConfig config) {
        excludedCheckBox.setSelected(config.isExcluded());
    }

    public CloverModuleConfig getConfig() {
        final CloverModuleConfig config = new CloverModuleConfig();
        config.setExcluded(excludedCheckBox.isSelected());
        return config;
    }

}
