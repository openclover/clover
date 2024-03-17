package org.openclover.eclipse.core.ui.widgets;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.openclover.core.context.ContextStore;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.projects.settings.RegexpEntry;
import org.openclover.eclipse.core.ui.SwtUtils;

import java.util.regex.Pattern;

class RegexpFilterEditDialog extends Dialog {
    private Text nameField;
    private Combo typeCombo;
    private Text regexpField;
    private java.util.List<RegexpEntry> regexps;
    private RegexpEntry data;
    private Label instructionsLabel;

    protected RegexpFilterEditDialog(Shell parentShell, java.util.List<RegexpEntry> regexps) {
        super(parentShell);
        setShellStyle(
            SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
        this.regexps = regexps;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Edit Regular Expression Filter");
    }

    protected void setData(RegexpEntry regexp) {
        data = regexp;
    }

    protected RegexpEntry getData() {
        return data;
    }

    @Override
    protected void okPressed() {

        String errorMessage = null;
        if (nameField.getText().trim().length() == 0) {
            errorMessage = "Context names must not be blank.";
        } else if (nameField.getText().indexOf(' ') != -1
            || nameField.getText().indexOf(',') != -1) {
            errorMessage = "Context names may not contain spaces or commas.";
        } else if (ContextStore.isReservedName(nameField.getText())) {
            errorMessage = "This context name is reserved for use by OpenClover.";
        } else {
            for (RegexpEntry regexpEntry : regexps) {
                if (regexpEntry != data && regexpEntry.getName().equals(nameField.getText())) {
                    errorMessage = "This context name is already in use.";
                }
            }
            if (errorMessage == null) {
                try {
                    Pattern.compile(regexpField.getText());
                } catch (Exception e) {
                    errorMessage = "The regular expression is not valid:\n" +  e.getMessage();
                }
            }
        }


        if (errorMessage != null) {
            MessageDialog.openError(getShell(), "Error", errorMessage);
        } else {
            data.setName(nameField.getText());
            data.setRegexp(regexpField.getText());
            if (typeCombo.getSelectionIndex() == 0) {
                data.setType(RegexpEntry.METHOD_TYPE);
            } else {
                data.setType(RegexpEntry.STATEMENT_TYPE);
            }
            data.setChanged(true);

            super.okPressed();
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite contents = (Composite) super.createDialogArea(parent);
        contents.setLayout(new GridLayout(2, false));

        Label nameLabel = new Label(contents, SWT.NONE);
        nameLabel.setText("Name:");

        nameField = new Text(contents, SWT.BORDER);
        nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label typeLabel = new Label(contents, SWT.NONE);
        typeLabel.setText("Type:");

        typeCombo = new Combo(contents, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
        typeCombo.add("Method");
        typeCombo.add("Statement");
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (typeCombo.getSelectionIndex() == 0) {
                    instructionsLabel.setText(
                        CloverEclipsePluginMessages.METHOD_CONTEXT_FILTER_INSTRUCTIONS());
                } else {
                    instructionsLabel.setText(
                        CloverEclipsePluginMessages.STATEMENT_CONTEXT_FILTER_INSTRUCTIONS());
                }
                RegexpFilterEditDialog.this.getShell().layout();
            }
        });
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label regexpLabel = new Label(contents, SWT.NONE);
        regexpLabel.setText("Regular Expression:");
        regexpLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        regexpField = new Text(contents, SWT.BORDER | SWT.MULTI);
        regexpField.setLayoutData(new GridData(GridData.FILL_BOTH));
        ((GridData)regexpField.getLayoutData()).widthHint = 200;
        ((GridData)regexpField.getLayoutData()).heightHint = 75;

        //Filler
        new Label(contents, SWT.NONE);

        int maxInstructionWidth =
            Math.max(
                CloverEclipsePluginMessages.METHOD_CONTEXT_FILTER_INSTRUCTIONS().length(),
                CloverEclipsePluginMessages.STATEMENT_CONTEXT_FILTER_INSTRUCTIONS().length());
        //Instructions on regex
        instructionsLabel = SwtUtils.createMultilineLabel(
                contents,
                CloverEclipsePluginMessages.METHOD_CONTEXT_FILTER_INSTRUCTIONS(),
                convertWidthInCharsToPixels(maxInstructionWidth/5));
        
        nameField.setText(data.getName());
        regexpField.setText(data.getRegexp());
        if (data.getType() == RegexpEntry.METHOD_TYPE) {
            typeCombo.select(0);
        } else {
            typeCombo.select(1);
        }

        return contents;
    }
}
