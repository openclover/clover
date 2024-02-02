package com.atlassian.clover.idea.report;

import com.atlassian.clover.idea.config.GBC;
import com.atlassian.clover.idea.util.ui.UIUtils;
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

public class ConfigureHtmlUI extends AbstractConfigureUI {

    private JTextArea helpText;
    private JTextField outputDir;
    private JLabel outputDirLabel;
    private JCheckBox includeSource;
    private JCheckBox includeFailedCoverageCheckBox;

    public ConfigureHtmlUI(ReportWizard wiz) {
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
        add(getOutputDirLabel(), new GBC(0, 3).setSpan(2, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(UIUtils.wrapWithFileChooser(wizard.getDialogWindow(), getOutputDir(), JFileChooser.DIRECTORIES_ONLY), new GBC(0, 4).setSpan(2, 1).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST).setWeight(0.01, 0.0));
        add(getIncludeSource(), new GBC(0, 5).setSpan(2, 1).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(getIncludeFailedCoverageCheckBox(), new GBC(0, 6).setSpan(2, 1).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(getUseFilters(), new GBC(0, 7).setSpan(2, 1).setFill(GBC.HORIZONTAL).setInsets(0, 5, 5, 5).setAnchor(GBC.NORTHWEST));
        add(getShowLambdaLabel(), new GBC(0, 8).setFill(GBC.HORIZONTAL).setInsets(0, 5, 5, 5).setAnchor(GBC.WEST));
        add(getShowLambdaCombo(), new GBC(1, 8).setFill(GBC.HORIZONTAL).setInsets(0, 5, 5, 5).setAnchor(GBC.WEST));
        add(new JPanel(), new GBC(0, 9).setSpan(2, 1).setFill(GBC.VERTICAL).setWeight(0.0, 0.1));
    }

    @Override
    protected void initListeners() {
        getUseFilters().addActionListener(e -> wizard.refreshState(!getUseFilters().isSelected()));
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("Please specify the report title " +
                    "and the output directory. The output directory will be the root " +
                    "directory of the generated report.",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private JTextField getOutputDir() {
        if (outputDir == null) {
            outputDir = new JTextField(20);
        }
        return outputDir;
    }

    private JLabel getOutputDirLabel() {
        if (outputDirLabel == null) {
            outputDirLabel = new JLabel("Output Directory:");
            outputDirLabel.setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
        }
        return outputDirLabel;
    }

    private JCheckBox getIncludeSource() {
        if (includeSource == null) {
            includeSource = new JCheckBox("Include sources");
        }
        return includeSource;
    }

    private JCheckBox getIncludeFailedCoverageCheckBox() {
        if (includeFailedCoverageCheckBox == null) {
            includeFailedCoverageCheckBox = new JCheckBox("Include failed test coverage");
        }
        return includeFailedCoverageCheckBox;
    }

    @Override
    public void writeConfig(WizardConfig reportConfig) {
        WizardConfig.HtmlReport config = reportConfig.getHtmlConfig();
        config.setReportTitle(getReportTitle().getText());
        config.setDir(new File(getOutputDir().getText()));
        config.setIncludeSource(getIncludeSource().isSelected());
        config.setIncludeFailedTestCoverage(getIncludeFailedCoverageCheckBox().isSelected());
        config.setShowLambda(
                getShowLambdaCombo().getSelectedIndex() == 2 ? ShowLambdaFunctions.FIELDS_AND_INLINE :
                        getShowLambdaCombo().getSelectedIndex() == 1 ? ShowLambdaFunctions.FIELDS_ONLY :
                                ShowLambdaFunctions.NONE
        );

        reportConfig.setUseCurrentFilterSettings(getUseFilters().isSelected());
    }

    @Override
    public void readConfig(WizardConfig reportConfig) {
        WizardConfig.HtmlReport config = reportConfig.getHtmlConfig();
        getReportTitle().setText(config.getReportTitle());
        getOutputDir().setText(config.getDir().getAbsolutePath());
        getIncludeSource().setSelected(config.isIncludeSource());
        getIncludeFailedCoverageCheckBox().setSelected(config.isIncludeFailedCoverage());
        getShowLambdaCombo().setSelectedIndex(config.getShowLambda().ordinal());
        getUseFilters().setSelected(reportConfig.isUseCurrentFilterSettings());
    }

    @Override
    public String validateSettings() {
        String txt = getOutputDir().getText();
        if (txt == null || txt.trim().length() == 0) {
            return "Please enter an output directory.";
        }
        return null;
    }
}