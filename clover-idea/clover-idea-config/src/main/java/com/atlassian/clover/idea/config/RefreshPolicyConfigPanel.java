package com.atlassian.clover.idea.config;

import com.atlassian.clover.idea.util.ui.UIUtils;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.util.ui.UIUtil;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

public class RefreshPolicyConfigPanel extends ConfigPanel implements ActionListener {

    private JTextArea helpText;
    private JRadioButton directedButton;
    private JRadioButton autoButton;
    private JRadioButton periodicButton;
    private ButtonGroup buttonGroup;
    private JTextField intervalField;
    private JLabel intervalLabelB;

    public RefreshPolicyConfigPanel() {
        initLayout();
        initListeners();

        refreshState();
    }

    private void initLayout() {

        setBorder(getEnabledBorder());

        setLayout(new VerticalFlowLayout());
        add(getHelpText());
        add(getManualButton());
        add(getAutoButton());

        final Box periodicPanel = Box.createHorizontalBox();

        periodicPanel.add(getPeriodicButton());
        periodicPanel.add(getIntervalInput());
        periodicPanel.add(getPostIntervalInputLabel());
        periodicPanel.add(Box.createHorizontalGlue());

        add(periodicPanel);
    }

    private void initListeners() {
        getManualButton().addActionListener(this);
        getAutoButton().addActionListener(this);
        getPeriodicButton().addActionListener(this);
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        config.setAutoRefresh(getAutoButton().isSelected());
        config.setPeriodicRefresh(getPeriodicButton().isSelected());
        String interval = getIntervalInput().getText();
        try {
            config.setAutoRefreshInterval(Integer.parseInt(interval));
        } catch (NumberFormatException e) {
            //TODO: need to indicate error to user somehow.
        }
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        getAutoButton().setSelected(config.isAutoRefresh());
        getPeriodicButton().setSelected(config.isPeriodicRefresh());
        getIntervalInput().setText("" + config.getAutoRefreshInterval());

        refreshState();
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("The Refresh Policy controls when and " +
                    "how frequently Clover looks for a change in coverage data.",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private JRadioButton getManualButton() {
        if (directedButton == null) {
            final URL image = getClass().getResource("/icons/sync.png");
            final String message = String.format("<html>Manual: press %s to refresh.",
                                                 image != null ? "<img src='" + image + "'>" : "Refresh");
            directedButton = new JRadioButton(message);
            getButtonGroup().add(directedButton);
            directedButton.setSelected(true);
        }
        return directedButton;
    }

    private JRadioButton getAutoButton() {
        if (autoButton == null) {
            autoButton = new JRadioButton("Automatic: after compilation or process finishes.");
            getButtonGroup().add(autoButton);
        }
        return autoButton;
    }

    private JRadioButton getPeriodicButton() {
        if (periodicButton == null) {
            periodicButton = new JRadioButton("Periodically every ");
            getButtonGroup().add(periodicButton);
        }
        return periodicButton;
    }

    private JLabel getPostIntervalInputLabel() {
        if (intervalLabelB == null) {
            intervalLabelB = new JLabel(" msecs");
        }
        return intervalLabelB;
    }

    private JTextField getIntervalInput() {
        if (intervalField == null) {
            intervalField = new JTextField();
            intervalField.setColumns(6);
            intervalField.setMaximumSize(intervalField.getPreferredSize());
        }
        return intervalField;
    }

    private ButtonGroup getButtonGroup() {
        if (buttonGroup == null) {
            buttonGroup = new ButtonGroup();
        }
        return buttonGroup;
    }

    @Override
    public String getTitle() {
        return "Refresh Policy";
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        final Object src = evt.getSource();
        if (src == getManualButton() || src == getAutoButton() || src == getPeriodicButton()) {
            refreshState();
        }
    }

    @Override
    public void enableConfig(boolean b) {
        super.enableConfig(b);
        getManualButton().setForeground(b ? UIUtil.getActiveTextColor() : UIUtil.getInactiveTextColor());
        if (b) {
            refreshState();
        }
    }

    private void refreshState() {
        final boolean enableIntervalInput = getPeriodicButton().isSelected() && isEnabled();

        getIntervalInput().setEnabled(enableIntervalInput);
        getPostIntervalInputLabel().setEnabled(enableIntervalInput);
    }
}
