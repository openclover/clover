package com.atlassian.clover.idea.report;

import javax.swing.JPanel;

public abstract class ReportWizardPage extends JPanel {

    protected final ReportWizard wizard;

    public ReportWizardPage(ReportWizard parent) {
        this.wizard = parent;
    }

    public abstract void writeConfig(WizardConfig reportConfig);

    public abstract void readConfig(WizardConfig reportConfig);

    public abstract String validateSettings();
}
