package com.atlassian.clover.eclipse.core.views.widgets.context;

import com.atlassian.clover.eclipse.core.projects.settings.ProjectSettings;
import com.atlassian.clover.eclipse.core.ui.widgets.ContextFilterModificationWidget;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

public class ContextChooserDialog extends Dialog {

    private ContextFilterModificationWidget contextControl;
    private ProjectSettings properties;

    public ContextChooserDialog(Shell parentShell, ProjectSettings properties) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
        this.properties = properties;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Coverage Context Filtering");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite base = (Composite)super.createDialogArea(parent);
        base.setLayout(new GridLayout());
        contextControl = new ContextFilterModificationWidget(base, properties);
        contextControl.setLayoutData(new GridData(GridData.FILL_BOTH));
        return base;
    }

    @Override
    protected void okPressed() {
        boolean rebuild = contextControl.alertIfManualRebuildRequired();
        contextControl.store();
        super.okPressed();
        if (rebuild) {
            try {
                properties.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to rebuild project", e);
            }
        }
    }
}
