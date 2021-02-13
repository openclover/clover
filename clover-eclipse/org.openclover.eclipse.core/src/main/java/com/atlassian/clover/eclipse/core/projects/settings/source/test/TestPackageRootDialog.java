package com.atlassian.clover.eclipse.core.projects.settings.source.test;

import com.atlassian.clover.eclipse.core.ui.SwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.SelectionDialog;

public class TestPackageRootDialog extends SelectionDialog {
    protected TestPackageRootDialog(Shell shell) {
        super(shell);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);

        shell.setText("Test source, clases and methods");
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite contents = new Composite(parent, SWT.NONE);
        contents.setLayout(new GridLayout(2, true));

        Label label = new Label(contents, SWT.NONE);
        label.setText("Source folders:");
        SwtUtils.setHorizontalSpan(label, 2);

        List folders = new List(contents,  SWT.BORDER);
        folders.setLayoutData(new GridData(GridData.FILL_BOTH));

        Button selectButton = new Button(contents, SWT.NONE);
        selectButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        selectButton.setText("Select");

        label = new Label(contents, SWT.NONE);
        label.setText("Included files:");
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.setHorizontalSpan(label, 2);

        Text includeText = new Text(contents, SWT.BORDER);
        includeText.setText("**/*.java");
        includeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.setHorizontalSpan(includeText, 2);

        label = new Label(contents, SWT.NONE);
        label.setText("Excluded files:");
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.setHorizontalSpan(label, 2);

        Text excludeText = new Text(contents, SWT.BORDER);
        excludeText.setText("");
        excludeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.setHorizontalSpan(excludeText, 2);

        label = new Label(contents, SWT.NONE);
        label.setText("Test classes:");
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.setHorizontalSpan(label, 2);

        Tree testClassTree = new Tree(contents, SWT.NONE);
        testClassTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        SwtUtils.setVerticalSpan(testClassTree, 3);

        Button addTestClassButton = new Button(contents, SWT.NONE);
        addTestClassButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        addTestClassButton.setText("Add");

        Button editTestClassButton = new Button(contents, SWT.NONE);
        editTestClassButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        editTestClassButton.setText("Edit");

        Button removeTestClassButton = new Button(contents, SWT.NONE);
        removeTestClassButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        removeTestClassButton.setText("Remove");

        return contents;
    }
}
