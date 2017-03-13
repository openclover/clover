package com.atlassian.clover.idea.config;

import com.atlassian.clover.idea.util.ui.UIUtils;
import com.intellij.util.ui.UIUtil;

import javax.swing.JPanel;
import javax.swing.border.Border;

public abstract class ConfigPanel extends JPanel {

    /**
     * Write the configuration data associated with this particular instance
     * of the config panel to the clover project data object.
     *
     * @param data config instance to store settings to
     */
    public abstract void commitTo(CloverPluginConfig data);

    /**
     * Initialise the configuration data presented by this particular instance
     * of the config panel using the data contained by the clover project data
     * object
     *
     * @param data config instance to read current settings from
     */
    public abstract void loadFrom(CloverPluginConfig data);

    /**
     * Get the panel title string.
     *
     * @return String
     */
    public abstract String getTitle();

    /**
     * Override if your panel does not have a border or is not a simple bunch of components added to the panel.
     *
     * @param enable enable config
     */
    public void enableConfig(boolean enable) {
        setBorder(enable ? getEnabledBorder() : getDisabledBorder());
        UIUtil.setEnabled(this, enable, true);
    }

    private Border enabledBorder;
    private Border disabledBorder;

    protected Border getEnabledBorder() {
        if (enabledBorder == null) {
            enabledBorder = UIUtils.createBorder(getTitle());
        }
        return enabledBorder;
    }

    protected Border getDisabledBorder() {
        if (disabledBorder == null) {
            disabledBorder = UIUtils.createDisabledBorder(getTitle());
        }
        return disabledBorder;
    }
}
