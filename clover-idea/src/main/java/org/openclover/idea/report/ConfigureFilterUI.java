package org.openclover.idea.report;

import org.openclover.runtime.Logger;
import org.openclover.core.context.ContextStore;
import org.openclover.idea.config.ContextPane;

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