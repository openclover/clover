package org.openclover.eclipse.core.reports;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import java.io.File;

public class ConfigurePdfPage extends ConfigureReportPage {

    public ConfigurePdfPage() {
        super("ConfigurePDF");
        setTitle("PDF Report Configuration");
        setDescription("Fine tune the available coverage report options.");
    }

    @Override
    protected String getDefaultOutputPathFor(IProject project) {
        return project.getFolder("report").getFile("coverage.pdf").getLocation().toFile().getAbsolutePath();
    }

    @Override
    protected void createCustomSettings(Composite composite) { }

    @Override
    protected Listener newOutputPathListener() {
        return event -> {
            Shell shell =((GenerateReportWizard)getWizard()).workbench.getActiveWorkbenchWindow().getShell();
            FileDialog chooser = new FileDialog(shell, SWT.PRIMARY_MODAL);
            chooser.setText("Choose a file");
            File enclosingDir = getOutput().getParentFile();
            if (enclosingDir != null) {
                chooser.setFilterPath(enclosingDir.getAbsolutePath());
            }
            String result = chooser.open();
            if (result != null) {
                setOutputPath(result);
            }
        };
    }

    @Override
    protected String getOutputPathLabelTitle() {
        return "Output File:";
    }

    @Override
    protected void addListeners() {
        Listener listener = newValidationListener();

        outputPath.addListener(SWT.KeyUp, listener);
        fileButton.addListener(SWT.Selection, listener);
        threadCount.addListener(SWT.Selection, listener);
    }

    @Override
    protected File getOutput() {
        return new File(outputPath.getText());
    }
}
