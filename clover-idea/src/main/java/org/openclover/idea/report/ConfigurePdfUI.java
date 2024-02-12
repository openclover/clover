package org.openclover.idea.report;

import org.openclover.idea.config.GBC;
import org.openclover.idea.util.ui.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.File;

public class ConfigurePdfUI extends AbstractConfigureUI {

    private JTextArea helpText;
    private JTextField reportTitle;
    private JTextField outputFile;
    private JLabel reportTitleLabel;
    private JLabel outputFileLabel;

    public ConfigurePdfUI(ReportWizard wiz) {
        super(wiz);
        initComponents();
        initListeners();
    }

    @Override
    protected void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEtchedBorder());

        add(getHelpText(), new GBC(0, 0).setInsets(5, 5, 5, 5).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(getReportTitleLabel(), new GBC(0, 1).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(getReportTitle(), new GBC(0, 2).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST).setWeight(0.01, 0.0));
        add(getOutputFileLabel(), new GBC(0, 3).setInsets(5, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST));
        add(UIUtils.wrapWithFileChooser(wizard.getDialogWindow(), getOutputFile(), JFileChooser.FILES_ONLY), new GBC(0, 4).setInsets(0, 5, 5, 5).setFill(GBC.HORIZONTAL).setAnchor(GBC.NORTHWEST).setWeight(0.01, 0.0));
        add(new JPanel(), new GBC(0, 5).setFill(GBC.VERTICAL).setWeight(0.0, 0.1));
        add(getUseFilters(), new GBC(0, 6).setFill(GBC.HORIZONTAL).setInsets(0, 5, 5, 5).setAnchor(GBC.NORTHWEST));
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

    @Override
    public void readConfig(WizardConfig reportConfig) {
        WizardConfig.PdfReport config = reportConfig.getPdfConfig();
        getReportTitle().setText(config.getReportTitle());
        getOutputFile().setText(config.getFile().getAbsolutePath());

        getUseFilters().setSelected(reportConfig.isUseCurrentFilterSettings());
    }

    @Override
    public void writeConfig(WizardConfig reportConfig) {
        WizardConfig.PdfReport config = reportConfig.getPdfConfig();
        config.setReportTitle(getReportTitle().getText());
        config.setFile(new File(getOutputFile().getText()));

        reportConfig.setUseCurrentFilterSettings(getUseFilters().isSelected());
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
