package org.openclover.idea.config;

import javax.swing.JCheckBox;
import java.awt.GridBagLayout;

public class EnabledConfigPanel extends ConfigPanel {

    private JCheckBox enabledCheckbox;

    public EnabledConfigPanel() {
        initComponent();
        initListeners();
    }

    private void initComponent() {

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        add(getEnabledInput(), new GBC(1, 1).setFill(GBC.BOTH).setWeight(1.0, 1.0));
    }

    private void initListeners() {
    }

    @Override
    public String getTitle() {
        return "Enable OpenClover";
    }

    public JCheckBox getEnabledInput() {
        if (enabledCheckbox == null) {
            enabledCheckbox = new JCheckBox("Enable OpenClover");
        }
        return enabledCheckbox;
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        config.setEnabled(getEnabledInput().isSelected());
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        getEnabledInput().setSelected(config.isEnabled());
    }

    @Override
    public void enableConfig(boolean b) {
        getEnabledInput().setEnabled(b);
    }

}
