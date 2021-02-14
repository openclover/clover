package org.openclover.eclipse.core.reports;

import org.openclover.eclipse.core.ui.SwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.resources.IProject;

public class ConfigureHtmlPage extends ConfigureReportPage {

    private Button srcButton;
    //  private Button useWorkingSetButton;

    public ConfigureHtmlPage() {
        super("ConfigureHTML");
        setTitle("HTML Report Configuration");
        setDescription("Fine tune the available coverage report options.");
    }

    @Override
    protected String getDefaultOutputPathFor(IProject project) {
        return project.getFolder("report").getFolder("html").getLocation().toFile().getAbsolutePath();
    }

    @Override
    protected Listener newOutputPathListener() {
        return new Listener() {
            @Override
            public void handleEvent(Event event) {
                Shell shell =((GenerateReportWizard)getWizard()).workbench.getActiveWorkbenchWindow().getShell();
                DirectoryDialog chooser = new DirectoryDialog(shell, SWT.PRIMARY_MODAL);
                chooser.setText("Choose a directory");
                chooser.setFilterPath(outputPath.getText());
                String result = chooser.open();
                if (result != null) {
                    setOutputPath(result);
                }
            }
        };
    }

    @Override
    protected String getOutputPathLabelTitle() {
        return "Output Directory:";
    }

    @Override
    protected void createCustomSettings(Composite composite) {
        srcButton = new Button(composite, SWT.CHECK);
        srcButton.setText("Include source");
        srcButton.setSelection(true);
        GridData gd = SwtUtils.gridDataFor(srcButton);
        gd.horizontalSpan = 3;
        gd.horizontalAlignment = GridData.BEGINNING;

        createShowLambdaComposite(composite);
//      useWorkingSetButton = new Button(composite, SWT.CHECK);
//      useWorkingSetButton.setText("Restrict report to current Clover working set");
//      useWorkingSetButton.setSelection(CloverPlugin.getInstance().isInWorkingSetMode());
//      useWorkingSetButton.setEnabled(CloverPlugin.getInstance().isInWorkingSetMode());
//      gd = new GridData();
//      gd.horizontalSpan = 3;
//      gd.horizontalAlignment = GridData.BEGINNING;
//      useWorkingSetButton.setLayoutData(gd);
    }

    @Override
    protected void addListeners() {
        Listener listener = newValidationListener();

        outputPath.addListener(SWT.KeyUp, listener);
        fileButton.addListener(SWT.Selection, listener);
        threadCount.addListener(SWT.Selection, listener);
    }

    protected boolean shouldIncludeSource() {
        return srcButton.getSelection();
    }
}
