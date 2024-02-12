package org.openclover.eclipse.core.reports;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.openclover.eclipse.core.ui.SwtUtils;

import java.io.File;

public class ConfigureXmlPage extends ConfigureReportPage {
    private Button lineInfoButton = null;

    public ConfigureXmlPage() {
        super("ConfigureXML");
        setTitle("XML Report Configuration");
        setDescription("Fine tune the available coverage report options.");
    }

    @Override
    protected String getOutputPathLabelTitle() {
        return "Output File:";
    }

    @Override
    protected String getDefaultOutputPathFor(IProject project) {
        return project.getProject().getFolder("report").getFile("coverage.xml").getLocation().toFile().getAbsolutePath();
    }

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
    protected void createCustomSettings(Composite composite) {
        lineInfoButton = new Button(composite, SWT.CHECK);
        lineInfoButton.setText("Include line coverage information in the report");
        lineInfoButton.setSelection(true);
        GridData gd = SwtUtils.gridDataFor(lineInfoButton);
        gd.horizontalSpan = 2;
        gd.horizontalAlignment = GridData.BEGINNING;

        createShowLambdaComposite(composite);
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

    public boolean isIncludingLineInfo() {
        return lineInfoButton.getSelection();
    }
}