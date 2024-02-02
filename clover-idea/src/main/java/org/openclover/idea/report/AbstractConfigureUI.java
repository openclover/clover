package org.openclover.idea.report;

import com.atlassian.clover.reporters.ShowLambdaFunctions;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Font;

/**
 *
 */
public abstract class AbstractConfigureUI extends ReportWizardPage {
    private JLabel reportTitleLabel;
    private JTextField reportTitle;
    private JLabel showLambdaLabel;
    private JComboBox showLambdaCombo;
    private JCheckBox useFilters;

    public AbstractConfigureUI(ReportWizard wiz) {
        super(wiz);
    }

    protected abstract void initListeners();

    protected abstract void initComponents();

    protected JLabel getReportTitleLabel() {
        if (reportTitleLabel == null) {
            reportTitleLabel = new JLabel("Report Title:");
            reportTitleLabel.setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
        }
        return reportTitleLabel;
    }

    protected JTextField getReportTitle() {
        if (reportTitle == null) {
            reportTitle = new JTextField(20);
        }
        return reportTitle;
    }

    protected JLabel getShowLambdaLabel() {
        if (showLambdaLabel == null) {
            showLambdaLabel = new JLabel("Show lambda functions:");
        }
        return showLambdaLabel;
    }

    protected JComboBox getShowLambdaCombo() {
        if (showLambdaCombo == null) {
            showLambdaCombo = new JComboBox(new String[] {
                    ShowLambdaFunctions.NONE.getDescription(),
                    ShowLambdaFunctions.FIELDS_ONLY.getDescription(),
                    ShowLambdaFunctions.FIELDS_AND_INLINE.getDescription()
            });
            showLambdaCombo.setEditable(false);
        }
        return showLambdaCombo;
    }

    protected JCheckBox getUseFilters() {
        if (useFilters == null) {
            useFilters = new JCheckBox("Use current filter settings");
        }
        return useFilters;
    }
}
