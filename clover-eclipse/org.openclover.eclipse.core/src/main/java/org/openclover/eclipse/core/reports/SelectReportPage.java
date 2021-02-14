package org.openclover.eclipse.core.reports;

import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.ui.GLH;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class SelectReportPage extends WizardPage {

    private Button htmlSelection = null;
    private Button pdfSelection = null;
    private Button xmlSelection = null;

    private CloverProject project;

    public SelectReportPage(CloverProject project) {
        super("SelectReport");
        setTitle("Report Format");
        setDescription("Choose the type of report you want to generate.");
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GLH().numColumns(1).getGridLayout());

        GenerateReportWizard wizard = (GenerateReportWizard) getWizard();

        Label label = new Label(composite, SWT.NONE);
        label.setText("Available report formats:");
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        label.setLayoutData(gd);

        htmlSelection = new Button(composite, SWT.RADIO);
        htmlSelection.setText("HTML Report");
        htmlSelection.setEnabled(wizard.isHtmlAvailable());
        gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        htmlSelection.setLayoutData(gd);

        pdfSelection = new Button(composite, SWT.RADIO);
        pdfSelection.setText("PDF Report");
        pdfSelection.setEnabled(wizard.isPdfAvailable());
        gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        pdfSelection.setLayoutData(gd);

        xmlSelection = new Button(composite, SWT.RADIO);
        xmlSelection.setText("XML Report");
        xmlSelection.setEnabled(wizard.isXmlAvailable());
        gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        xmlSelection.setLayoutData(gd);

        htmlSelection.setSelection(true);
        // set the composite as the control of this page.
        setControl(composite);
    }

    @Override
    public IWizardPage getNextPage() {
        return ((GenerateReportWizard) getWizard()).selectProjectsPage;
    }

    @Override
    public boolean canFlipToNextPage() {
        return true;
    }

    /**
     * Has the user requested an html style report?
     * @return
     */
    protected boolean isHtmlSelected() {
        return htmlSelection.getSelection();
    }

    /**
     * Has the user requested a pdf style report?
     * @return
     */
    protected boolean isPdfSelected() {
        return pdfSelection.getSelection();
    }

    /**
     * Has the user requested an xml style report?
     * @return
     */
    protected boolean isXmlSelected() {
        return xmlSelection.getSelection();
    }
}
