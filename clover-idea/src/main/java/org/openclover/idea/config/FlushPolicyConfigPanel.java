package com.atlassian.clover.idea.config;

import com.atlassian.clover.idea.util.ui.UIUtils;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FlushPolicyConfigPanel extends ConfigPanel implements ActionListener {

    private JTextArea helpText;
    private JRadioButton directedButton;
    private JRadioButton intervalButton;
    private JRadioButton threadedButton;
    private ButtonGroup buttonGroup;
    private JLabel intervalLabelA;
    private JTextField intervalField;
    private JLabel intervalLabelB;

    public FlushPolicyConfigPanel() {
        initLayout();
        initListeners();
    }

    private void initLayout() {
        setBorder(getEnabledBorder());

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        add(getHelpText(), new GBC(1, 1).setInsets(0, 6, 10, 6).setSpan(3, 1).setFill(GBC.HORIZONTAL));
        add(getDirectedButton(), new GBC(1, 2).setInsets(0, 6, 0, 6).setFill(GBC.HORIZONTAL));
        add(getIntervalButton(), new GBC(1, 3).setInsets(0, 6, 0, 0).setFill(GBC.HORIZONTAL));
        add(getPreIntervalInputLabel(), new GBC(2, 3).setInsets(0, 0, 0, 6).setSpan(2, 1).setAnchor(GBC.WEST).setWeight(1.0, 0.0));
        add(getThreadedButton(), new GBC(1, 4).setInsets(0, 6, 6, 0).setFill(GBC.HORIZONTAL));
        add(getIntervalInput(), new GBC(2, 4).setInsets(0, 0, 6, 0).setFill(GBC.HORIZONTAL).setWeight(1, 0));
        add(getPostIntervalInputLabel(), new GBC(3, 4).setInsets(0, 0, 6, 6).setFill(GBC.HORIZONTAL));
    }

    private void initListeners() {
        getDirectedButton().addActionListener(this);
        getIntervalButton().addActionListener(this);
        getThreadedButton().addActionListener(this);
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        if (getDirectedButton().isSelected()) {
            config.setFlushPolicy(CloverPluginConfig.DIRECTED_FLUSHING);
        } else if (getIntervalButton().isSelected()) {
            config.setFlushPolicy(CloverPluginConfig.INTERVAL_FLUSHING);
        } else if (getThreadedButton().isSelected()) {
            config.setFlushPolicy(CloverPluginConfig.THREADED_FLUSHING);
        }
        String interval = getIntervalInput().getText();
        try {
            config.setFlushInterval(Integer.parseInt(interval));
        } catch (NumberFormatException e) {
            //TODO: need to indicate error to user somehow.
        }
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        switch (config.getFlushPolicy()) {
            case CloverPluginConfig.INTERVAL_FLUSHING:
                getIntervalButton().setSelected(true);
                break;
            case CloverPluginConfig.THREADED_FLUSHING:
                getThreadedButton().setSelected(true);
                break;
            default: //CloverPluginConfig.DIRECTED_FLUSHING:
                getDirectedButton().setSelected(true);
        }
        getIntervalInput().setText("" + config.getFlushInterval());

        refreshState();
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("The Flush Policy controls " +
                    "how Clover writes coverage data to disk at runtime.",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private JRadioButton getDirectedButton() {
        if (directedButton == null) {
            directedButton = new JRadioButton("Directed");
            getButtonGroup().add(directedButton);
            directedButton.setSelected(true);
        }
        return directedButton;
    }

    private JRadioButton getIntervalButton() {
        if (intervalButton == null) {
            intervalButton = new JRadioButton("Interval");
            getButtonGroup().add(intervalButton);
        }
        return intervalButton;
    }

    private JRadioButton getThreadedButton() {
        if (threadedButton == null) {
            threadedButton = new JRadioButton("Threaded     ");
            getButtonGroup().add(threadedButton);
        }
        return threadedButton;
    }

    private JLabel getPreIntervalInputLabel() {
        if (intervalLabelA == null) {
            intervalLabelA = new JLabel("Flush interval");
        }
        return intervalLabelA;
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
        }
        return intervalField;
    }

    private ButtonGroup getButtonGroup() {
        if (buttonGroup == null) {
            buttonGroup = new ButtonGroup();
        }
        return buttonGroup;
    }

//    private String getDefaultToolTip() {
//        return "The flush policy configuration is disabled until " +
//                "clover instrumentation is supported by the build process.";
//    }

    @Override
    public String getTitle() {
        return "Flush Policy";
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == getDirectedButton() ||
                evt.getSource() == getIntervalButton() ||
                evt.getSource() == getThreadedButton()) {
            refreshState();
        }
    }

    @Override
    public void enableConfig(boolean b) {
        if (b) {
            setBorder(getEnabledBorder());
        } else {
            setBorder(getDisabledBorder());
        }
        getHelpText().setEnabled(b);
        getDirectedButton().setEnabled(b);
        getIntervalButton().setEnabled(b);
        getIntervalInput().setEnabled(b);
        getPreIntervalInputLabel().setEnabled(b);
        getPostIntervalInputLabel().setEnabled(b);
        getThreadedButton().setEnabled(b);
        setEnabled(b);

        if (b) {
            refreshState();
        }
    }

    private void refreshState() {
        if (getDirectedButton().isSelected()) {
            getIntervalInput().setEnabled(false);
            getPreIntervalInputLabel().setEnabled(false);
            getPostIntervalInputLabel().setEnabled(false);
        } else if (getIntervalButton().isSelected()) {
            getIntervalInput().setEnabled(true);
            getPreIntervalInputLabel().setEnabled(true);
            getPostIntervalInputLabel().setEnabled(true);
        } else if (getThreadedButton().isSelected()) {
            getIntervalInput().setEnabled(true);
            getPreIntervalInputLabel().setEnabled(true);
            getPostIntervalInputLabel().setEnabled(true);
        }
    }


}
