package org.openclover.idea.report;

import org.openclover.idea.config.GBC;
import org.openclover.idea.util.ui.UIUtils;
import com.atlassian.clover.reporters.ShowLambdaFunctions;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ConfigureXmlUI extends AbstractConfigureUI {

    private JTextArea helpText;
    private JTextField outputFile;
    private JLabel outputFileLabel;
    private JCheckBox includeLineInfo;

    public ConfigureXmlUI(ReportWizard wiz) {
        super(wiz);
        initComponents();
        initListeners();
    }

    @Override
    protected void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEtchedBorder());

        add(getHelpText(), new GBC(0, 0).setSpan(2, 1).setInsets(5, 5, 5, 5).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(getReportTitleLabel(), new GBC(0, 1).setSpan(2, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(getReportTitle(), new GBC(0, 2).setSpan(2, 1).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST).setWeight(0.01, 0.0));
        add(getOutputFileLabel(), new GBC(0, 3).setSpan(2, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(UIUtils.wrapWithFileChooser(wizard.getDialogWindow(), getOutputFile(), JFileChooser.FILES_ONLY), new GBC(0, 4).setSpan(2, 1).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST).setWeight(0.01, 0.0));
        add(getIncludeLineInfo(), new GBC(0, 5).setSpan(2, 1).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(getUseFilters(), new GBC(0, 6).setSpan(2, 1).setFill(GBC.HORIZONTAL).setInsets(0, 5, 5, 5).setAnchor(GBC.NORTHWEST));
        add(getShowLambdaLabel(), new GBC(0, 7).setFill(GBC.HORIZONTAL).setInsets(0, 5, 5, 5).setAnchor(GBC.WEST));
        add(getShowLambdaCombo(), new GBC(1, 7).setFill(GBC.HORIZONTAL).setInsets(0, 5, 5, 5).setAnchor(GBC.WEST));
        add(new JPanel(), new GBC(0, 8).setSpan(2, 1).setFill(GBC.VERTICAL).setWeight(0.0, 0.1));
    }

    @Override
    protected void initListeners() {
        getUseFilters().addActionListener(e -> wizard.refreshState(!getUseFilters().isSelected()));
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("Please specify the report title " +
                    "and the output file. The output file is the file used for " +
                    "the generated report.",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private JTextField getOutputFile() {
        if (outputFile == null) {
            outputFile = new JTextField(20);
        }
        return outputFile;
    }

    private JLabel getOutputFileLabel() {
        if (outputFileLabel == null) {
            outputFileLabel = new JLabel("Output File:");
            outputFileLabel.setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
        }
        return outputFileLabel;
    }

    private JCheckBox getIncludeLineInfo() {
        if (includeLineInfo == null) {
            includeLineInfo = new JCheckBox("Include line info:");
        }
        return includeLineInfo;
    }

    @Override
    public void writeConfig(WizardConfig reportConfig) {
        WizardConfig.XmlReport config = reportConfig.getXmlConfig();
        config.setReportTitle(getReportTitle().getText());
        config.setFile(new File(getOutputFile().getText()));
        config.setIncludeLineInfo((getIncludeLineInfo().isSelected()));
        config.setShowLambda(
                getShowLambdaCombo().getSelectedIndex() == 2 ? ShowLambdaFunctions.FIELDS_AND_INLINE :
                        getShowLambdaCombo().getSelectedIndex() == 1 ? ShowLambdaFunctions.FIELDS_ONLY :
                                ShowLambdaFunctions.NONE
        );

        reportConfig.setUseCurrentFilterSettings(getUseFilters().isSelected());
    }

    @Override
    public void readConfig(WizardConfig reportConfig) {
        WizardConfig.XmlReport config = reportConfig.getXmlConfig();
        getReportTitle().setText(config.getReportTitle());
        getOutputFile().setText(config.getFile().getAbsolutePath());
        getIncludeLineInfo().setSelected(config.isIncludeLineInfo());
        getShowLambdaCombo().setSelectedIndex(config.getShowLambda().ordinal());
        getUseFilters().setSelected(reportConfig.isUseCurrentFilterSettings());
    }

    @Override
    public String validateSettings() {
        String txt = getOutputFile().getText();
        if (txt == null || txt.trim().length() == 0) {
            return "Please enter an output file.";
        }
        return null;
    }
}
