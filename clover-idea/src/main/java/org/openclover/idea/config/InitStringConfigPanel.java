package org.openclover.idea.config;

import org.openclover.idea.IDEContext;
import org.openclover.idea.util.ui.UIUtils;
import com.atlassian.clover.util.FileUtils;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class InitStringConfigPanel extends ConfigPanel implements ActionListener {

    private InitStringTextInput instInput;
    private JButton browseButton;
    private JTextArea helpText;

    private JRadioButton automaticButton;
    private JRadioButton manualButton;
    private JCheckBox relativeBox;

    private ButtonGroup buttonGroup;

    private IDEContext context;

    public InitStringConfigPanel(IDEContext ctx) {
        this.context = ctx;
        initLayout();
        initListeners();
        refreshState();
    }

    private void initLayout() {
        setBorder(getEnabledBorder());

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        add(getHelpText(), new GBC(1, 1).setInsets(0, 6, 10, 6).setSpan(3, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 1.0));
        add(getAutomaticButton(), new GBC(1, 2).setInsets(0, 6, 0, 6).setSpan(2, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 1.0));
        add(getManualButton(), new GBC(1, 3).setInsets(0, 6, 0, 6).setFill(GBC.HORIZONTAL).setWeight(1.0, 1.0));
        add(getInitStrInput(), new GBC(1, 4).setInsets(0, 6, 0, 0).setFill(GBC.HORIZONTAL).setWeight(1.0, 1.0));
        add(getBrowseButton(), new GBC(2, 4).setInsets(0, 0, 0, 6).setAnchor(GBC.WEST).setWeight(1.0, 1.0));
        add(getRelativeCheckBox(), new GBC(1, 5).setSpan(2, 1).setInsets(0, 6, 6, 6).setFill(GBC.HORIZONTAL).setWeight(1.0, 1.0));
        add(new JPanel(), new GBC(1, 6).setSpan(2, 1).setFill(GBC.BOTH).setWeight(0.0, 1.0));

    }

    private void initListeners() {
        getBrowseButton().addActionListener(this);
        getAutomaticButton().addActionListener(this);
        getManualButton().addActionListener(this);
        getRelativeCheckBox().addActionListener(this);
    }

    @Override
    public String getTitle() {
        return "InitString";
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("The Initstring specifies " +
                    "the name of the coverage database file. Select " +
                    "'Automatic' to have Clover manage this location for you.",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private InitStringTextInput getInitStrInput() {
        if (instInput == null) {
            instInput = new InitStringTextInput();
            instInput.setColumns(20);
        }
        return instInput;
    }

    private JButton getBrowseButton() {
        if (browseButton == null) {
            browseButton = new JButton();
            browseButton.setText("...");
        }
        return browseButton;
    }

    private JRadioButton getAutomaticButton() {
        if (automaticButton == null) {
            automaticButton = new JRadioButton("Automatic");
            getButtonGroup().add(automaticButton);
        }
        return automaticButton;
    }

    private JRadioButton getManualButton() {
        if (manualButton == null) {
            manualButton = new JRadioButton("User specified:");
            getButtonGroup().add(manualButton);
            manualButton.setSelected(true);
        }
        return manualButton;
    }

    private ButtonGroup getButtonGroup() {
        if (buttonGroup == null) {
            buttonGroup = new ButtonGroup();
        }
        return buttonGroup;
    }

    private JCheckBox getRelativeCheckBox() {
        if (relativeBox == null) {
            relativeBox = new JCheckBox("relative to project directory.");
        }
        return relativeBox;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == getBrowseButton()) {
            JFileChooser chooser = new JFileChooser();

            boolean relativeToProjectBase = getRelativeCheckBox().isSelected();
            if (relativeToProjectBase) {
                try {
                    File dir = context.getProjectRootDirectory();
                    if (dir != null) {
                        chooser.setCurrentDirectory(dir);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                getInitStrInput().setInitStr(selectedFile);
            }
        } else if (evt.getSource() == getAutomaticButton() || evt.getSource() == getManualButton()) {
            refreshState();
        } else if (evt.getSource() == getRelativeCheckBox()) {
            getInitStrInput().setRelativeToProjectDir(getRelativeCheckBox().isSelected());
        }
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        config.setManualInitString(getInitStrInput().getInitStr());
        config.setUseGeneratedInitString(getAutomaticButton().isSelected());
        config.setRelativeInitString(getRelativeCheckBox().isSelected());
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        getAutomaticButton().setSelected(config.getUseGeneratedInitString());
        getRelativeCheckBox().setSelected(config.isRelativeInitString());
        getInitStrInput().setRelativeToProjectDir(config.isRelativeInitString());
        getInitStrInput().setInitStr(config.getManualInitString());
        refreshState();
    }

    /**
     * @param b boolean
     */
    @Override
    public void enableConfig(boolean b) {
        if (b) {
            setBorder(getEnabledBorder());
        } else {
            setBorder(getDisabledBorder());
        }

        getHelpText().setEnabled(b);
        getAutomaticButton().setEnabled(b);
        getManualButton().setEnabled(b);
        if (b) {
            refreshState();
        } else {
            getInitStrInput().setEnabled(false);
            getBrowseButton().setEnabled(false);
            getRelativeCheckBox().setEnabled(false);
        }
    }

    private void refreshState() {
        if (getAutomaticButton().isSelected()) {
            getInitStrInput().setEnabled(false);
            getBrowseButton().setEnabled(false);
            getRelativeCheckBox().setEnabled(false);
        } else {
            getInitStrInput().setEnabled(true);
            getBrowseButton().setEnabled(true);
            getRelativeCheckBox().setEnabled(true);
        }
    }

    private class InitStringTextInput extends JTextField {

        private boolean isRelative;

        public void setRelativeToProjectDir(boolean b) {
            if (b == isRelative) {
                return;
            }
            // update state.
            String initStr = getInitStr();
            isRelative = b;
            setInitStr(initStr);
        }

        public boolean isRelativeToProjectDir() {
            return isRelative;
        }

        public void setInitStr(File f) {
            setInitStr(f.getAbsolutePath());
        }

        public void setInitStr(String s) {
            String text = null;
            if (s != null) {
                if (isRelativeToProjectDir()) {
                    text = FileUtils.getRelativePath(context.getProjectRootDirectory().getAbsolutePath(), s);
                } else {
                    text = s;
                }
            }
            setText(text);
        }

        public String getInitStr() {
            String text = getText();
            if (text == null || text.trim().length() == 0) {
                return null;
            }

            // read and convert the current text value.
            final File initStr;
            if (isRelativeToProjectDir()) {
                initStr = new File(context.getProjectRootDirectory(), text);
            } else {
                initStr = new File(text);
            }

//            return FileUtils.normalize(initStr.getAbsolutePath()).getAbsolutePath();
            return initStr.getAbsolutePath();
        }
    }
}

