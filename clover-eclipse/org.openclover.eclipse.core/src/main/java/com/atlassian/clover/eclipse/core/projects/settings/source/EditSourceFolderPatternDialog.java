package com.atlassian.clover.eclipse.core.projects.settings.source;

import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditSourceFolderPatternDialog extends TrayDialog {

    private Text includeFilterText;
    private Text excludeFilterText;

    private final SourceFolderPattern initialPattern;
    private SourceFolderPattern resultPattern;

    protected EditSourceFolderPatternDialog(Shell parentShell, SourceFolderPattern initialPattern) {
        super(parentShell);
        this.initialPattern = initialPattern;

        setHelpAvailable(false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite control = (Composite) super.createDialogArea(parent);

        new Label(control, SWT.NONE).setText(CloverEclipsePluginMessages.FILE_FILTERING_INCLUDE());

        includeFilterText = new Text(control, SWT.BORDER);
        includeFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(control, SWT.NONE).setText(CloverEclipsePluginMessages.FILE_FILTERING_EXCLUDE());

        excludeFilterText = new Text(control, SWT.BORDER);
        excludeFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        getShell().setText("Patterns for: " + initialPattern.getSrcPath());
        includeFilterText.setText(initialPattern.getIncludePattern());
        excludeFilterText.setText(initialPattern.getExcludePattern());

        return control;
    }

    @Override
    protected void okPressed() {
        resultPattern = new SourceFolderPattern(initialPattern.getSrcPath(),
                                                                includeFilterText.getText(),
                                                                excludeFilterText.getText(),
                                                                initialPattern.isEnabled());
        super.okPressed();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public SourceFolderPattern getResult() {
        return resultPattern;
    }
}
