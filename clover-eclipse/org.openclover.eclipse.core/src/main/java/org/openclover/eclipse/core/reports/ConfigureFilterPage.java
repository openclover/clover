package org.openclover.eclipse.core.reports;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.openclover.eclipse.core.ui.widgets.BuiltinContextFilterSelectionWidget;
import org.openclover.eclipse.core.ui.widgets.ContextFilterSelectionWidget;
import org.openclover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.ContextStore;

public class ConfigureFilterPage extends WizardPage {
    private BuiltinContextFilterSelectionWidget contextWidget;
    private boolean flipResize;
    ContextStore contextRegistry;
    ContextSet filter;

    public ConfigureFilterPage() {
        super("ConfigureFilter");
        setTitle("Report Filter Configuration");
        setDescription("Select what to exclude from your coverage report.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new RowLayout(SWT.VERTICAL));
        setControl(composite);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            if (contextWidget != null && !contextWidget.isDisposed()) {
                contextWidget.dispose();
            }
            ConfigureReportPage reportConfigPage = ((GenerateReportWizard) getWizard()).getReportTypeWizard();

            CloverProject[] selectedProjects = ((GenerateReportWizard) getWizard()).selectProjectsPage.getSelectedProjects();
            if (selectedProjects.length == 1) {
                contextRegistry = selectedProjects[0].getSettings().getContextRegistry();
                filter = selectedProjects[0].getSettings().getContextFilter();
            } else {
                contextRegistry = new ContextStore();
                filter = new ContextSet();
            }

            contextWidget =
                new ContextFilterSelectionWidget(
                    (Composite) getControl(),
                    contextRegistry,
                    filter);
            contextWidget.buildContents();
            contextWidget.updateSelection();

            //We need to bump the bounds to force proper redrawing after removing then adding
            //a component :( So we add / remove a pixel to the height each time the sheet is shown 
            Rectangle bounds = getShell().getBounds();
            bounds.height += flipResize ? 1 : -1;
            getShell().setBounds(bounds);
            flipResize = !flipResize;
        }
        super.setVisible(visible);
    }

    public ContextSet getBlockContextFilter() {
        return contextWidget == null ? new ContextSet() : contextWidget.getFilterFromSelection();
    }

    public ContextStore getContextRegistry() {
        return contextRegistry == null ? new ContextStore() : contextRegistry;
    }

    @Override
    public IWizardPage getNextPage() {
        return ((GenerateReportWizard) getWizard()).configureJvmPage;
    }

    @Override
    public boolean canFlipToNextPage() {
        return true;
    }
}
