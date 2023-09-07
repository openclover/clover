package com.atlassian.clover.idea.report;

import com.atlassian.clover.Logger;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.idea.config.ContextPane;

import java.awt.BorderLayout;

public class ConfigureFilterUI extends ReportWizardPage {

    private Logger LOG = Logger.getInstance(ReportWizard.class.getName());


    private ContextPane contextFilterPane;

    public ConfigureFilterUI(ReportWizard wiz) {
        super(wiz);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        // support for built-in contexts only
        ContextStore contextStore = new ContextStore();
        contextFilterPane = new ContextPane(contextStore);
        add(contextFilterPane, BorderLayout.CENTER);
    }

    @Override
    public void writeConfig(WizardConfig config) {
        config.setContextSpec(contextFilterPane.getContextFilterSpec());
    }

    @Override
    public void readConfig(WizardConfig config) {
        contextFilterPane.setContextFilterSpec(config.getContextSpec());
    }

    @Override
    public String validateSettings() {
        return null;
    }
}