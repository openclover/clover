package com.atlassian.clover.eclipse.core.reports;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import com.atlassian.clover.eclipse.core.ui.SwtUtils;

public class ConfigureJvmPage extends WizardPage {
    private static final String[] VM_SIZES = new String[] {
        "256m",
        "512m",
        "768m",
        "1024m",
        "2048m"
    };

    private static final String[] VM_SIZE_NAMES = new String[] {
        "256 MB",
        "512 MB",
        "768 MB",
        "1024 MB",
        "2048 MB"
    };

    private Text jvmArgsText;
    private Combo jvmMaxHeapCombo;

    public ConfigureJvmPage() {
        super("ConfigureJvm");
        setTitle("Report JVM Configuration");
        setDescription("Fine-tune the Java Virtual Machine used for reporting, if required.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite settings = new Composite(parent, SWT.NONE);
        settings.setLayout(new GridLayout(2, false));

        SwtUtils.gridDataFor(settings).horizontalSpan = 2;

        Label heapLabel = new Label(settings, SWT.NONE);
        heapLabel.setText("Maximum heap size:");
        heapLabel.setToolTipText(
            "Sets the maximum memory heap size for the created Java Virtual Machine. " +
            "Increasing this for medium to large projects or where you are reporting " +
            "on more than one project at a time may speed up the time taken for reporting.");

        jvmMaxHeapCombo = new Combo(settings, SWT.READ_ONLY);
        jvmMaxHeapCombo.setItems(VM_SIZE_NAMES);
        jvmMaxHeapCombo.select(1);

        Label argLabel = new Label(settings, SWT.NONE);
        argLabel.setText("Additional JVM arguments:");
        argLabel.setToolTipText(
            "Supplies additional arguments to the the created Java Virtual Machine. ");

        jvmArgsText = new Text(settings, SWT.BORDER);
        jvmArgsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        setControl(settings);
    }

    public String getJvmArgs() {
        return jvmArgsText.getText();
    }

    public String getMaxHeapSize() {
        return VM_SIZES[jvmMaxHeapCombo.getSelectionIndex()];
    }

    @Override
    public IWizardPage getNextPage() {
        return null;
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }
}
