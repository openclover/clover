package org.openclover.eclipse.core.views.widgets.columns;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.CustomColumnDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ColumnSelectionDialog extends Dialog {
    private static final int IMPORT_ID = IDialogConstants.CLIENT_ID + 1;
    private static final int EXPORT_ID = IDialogConstants.CLIENT_ID + 2;

    private static final int BUTTON_WIDTH_HINT = 80;
    private static final int TABLE_WIDTH_HINT = 200;
    private static final int TABLE_RHS_HEIGHT_HINT = 300;
    private static final int TABLE_LHS_HEIGHT_HINT = 200;
    
    private ColumnDefinitionsModel model;
    private String viewName;
    private Table selectedColumnsTable;
    private TableViewer selectedColumnsTableViewer;
    private Table allBuiltinColumnsTable;
    private TableViewer allBuiltinColumnsTableViewer;
    private Table allCustomColumnsTable;
    private TableViewer allCustomColumnsTableViewer;
    private Button upButton;
    private Button allLeftButton;
    private Button downButton;
    private Button leftButton;
    private Button removeCustomUnassignedButton;
    private Button editCustomUnassignedButton;
    private Button newCustomButton;
    private Button customAllRightButton;
    private Button customRightButton;
    private Button builtinAllRightButton;
    private Button builtinRightButton;
    private Button editCustomAssignedButton;

    public ColumnSelectionDialog(
        Shell parentShell,
        String viewName,
        ColumnDefinition[] selectedColumns,
        ColumnDefinition[] allColumns) {
        super(parentShell);
        this.viewName = viewName;
        this.model = new ColumnDefinitionsModel(selectedColumns, allColumns);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(viewName + " Column Chooser");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
//        createButton(parent, IMPORT_ID, "Import...", false);
//        createButton(parent, EXPORT_ID, "Export...", false);
        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
        if (buttonId == IMPORT_ID) {
            doImport();
        } else if (buttonId == EXPORT_ID) {
            doExport();
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        composite.setLayout(new GridLayout(3, false));

        Composite availableColumns = new Composite(composite, SWT.NONE);
        availableColumns.setLayout(new GridLayout(2, false));
        availableColumns.setLayoutData(new GridData(GridData.FILL_BOTH));

        new Label(availableColumns, SWT.NONE).setText("Available built-in columns:");
        new Label(availableColumns, SWT.NONE);
        allBuiltinColumnsTable = new Table(availableColumns, SWT.BORDER);
        allBuiltinColumnsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        SwtUtils.gridDataFor(allBuiltinColumnsTable).verticalSpan = 2;
        SwtUtils.gridDataFor(allBuiltinColumnsTable).heightHint = TABLE_LHS_HEIGHT_HINT;
        SwtUtils.gridDataFor(allBuiltinColumnsTable).widthHint = TABLE_WIDTH_HINT;
        allBuiltinColumnsTableViewer = new TableViewer(allBuiltinColumnsTable);
        allBuiltinColumnsTableViewer.setLabelProvider(new ColumnDefinitionLabelProvider());
        allBuiltinColumnsTableViewer.setContentProvider(new ColumnDefinitionsProvider() {
            @Override
            public Object[] getElements(Object root) {
                final Object[] columns = model.getRemainingBuiltins().toArray();
                Arrays.sort(columns, ColumnDefinition.TITLE_COMPARATOR);
                return columns;
            }
        });
        allBuiltinColumnsTableViewer.setInput(model);
        allBuiltinColumnsTableViewer.addSelectionChangedListener(event -> updateButtonStates());
        builtinRightButton = new Button(availableColumns, SWT.NONE);
        builtinRightButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(builtinRightButton).widthHint = BUTTON_WIDTH_HINT;
        builtinRightButton.setText(">");
        builtinRightButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.assign(
                    (ColumnDefinition)allBuiltinColumnsTable.getSelection()[0].getData());
            }
        });
        builtinRightButton.setEnabled(false);

        builtinAllRightButton = new Button(availableColumns, SWT.NONE);
        builtinAllRightButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(builtinAllRightButton).widthHint = BUTTON_WIDTH_HINT;
        builtinAllRightButton.setText(">>");
        builtinAllRightButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.assignAll(false);
            }
        });

        new Label(availableColumns, SWT.NONE).setText("Available custom columns:");
        new Label(availableColumns, SWT.NONE);
        allCustomColumnsTable = new Table(availableColumns, SWT.BORDER);
        allCustomColumnsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        SwtUtils.gridDataFor(allCustomColumnsTable).verticalSpan = 5;
        SwtUtils.gridDataFor(allCustomColumnsTable).heightHint = TABLE_LHS_HEIGHT_HINT;
        SwtUtils.gridDataFor(allCustomColumnsTable).widthHint = TABLE_WIDTH_HINT;
        allCustomColumnsTableViewer = new TableViewer(allCustomColumnsTable);

        allCustomColumnsTableViewer.setLabelProvider(new ColumnDefinitionLabelProvider());
        allCustomColumnsTableViewer.setContentProvider(new ColumnDefinitionsProvider() {
            @Override
            public Object[] getElements(Object root) {
                final Object[] columns = model.getRemainingCustoms().toArray();
                Arrays.sort(columns, ColumnDefinition.TITLE_COMPARATOR);
                return columns;
            }
        });
        allCustomColumnsTableViewer.setInput(model);
        allCustomColumnsTableViewer.addSelectionChangedListener(event -> updateButtonStates());

        customRightButton = new Button(availableColumns, SWT.NONE);
        customRightButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_END));
        SwtUtils.gridDataFor(customRightButton).widthHint = BUTTON_WIDTH_HINT;
        customRightButton.setText(">");
        customRightButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.assign(
                    (ColumnDefinition)allCustomColumnsTable.getSelection()[0].getData());
            }
        });
        customRightButton.setEnabled(false);

        customAllRightButton = new Button(availableColumns, SWT.NONE);
        customAllRightButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(customAllRightButton).widthHint = BUTTON_WIDTH_HINT;
        customAllRightButton.setText(">>");
        customAllRightButton.setEnabled(false);
        customAllRightButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.assignAll(true);
            }
        });

        newCustomButton = new Button(availableColumns, SWT.NONE);
        newCustomButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(newCustomButton).widthHint = BUTTON_WIDTH_HINT;
        newCustomButton.setText("New");
        newCustomButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                CustomColumnDialog dialog = new CustomColumnDialog(getShell(), model.getBuiltintColumns());
                if (dialog.open() == Dialog.OK) {
                    model.addCustomColumn(dialog.getColumn());
                }
            }
        });

        editCustomUnassignedButton = new Button(availableColumns, SWT.NONE);
        editCustomUnassignedButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(editCustomUnassignedButton).widthHint = BUTTON_WIDTH_HINT;
        editCustomUnassignedButton.setText("Edit");
        editCustomUnassignedButton.setEnabled(false);
        editCustomUnassignedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                CustomColumnDefinition originalDefinition = (CustomColumnDefinition) allCustomColumnsTable.getSelection()[0].getData();
                CustomColumnDialog dialog = new CustomColumnDialog(getShell(), model.getBuiltintColumns(), originalDefinition);
                if (dialog.open() == Dialog.OK) {
                    model.replaceCustomColumn(originalDefinition, dialog.getColumn());
                }
            }
        });

        removeCustomUnassignedButton = new Button(availableColumns, SWT.NONE);
        removeCustomUnassignedButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(removeCustomUnassignedButton).widthHint = BUTTON_WIDTH_HINT;
        removeCustomUnassignedButton.setText("Delete");
        removeCustomUnassignedButton.setEnabled(false);
        removeCustomUnassignedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (MessageDialog.openConfirm(getShell(), "Delete Custom Column",
                        "Are you sure you wish to delete the custom column? It will be removed from any OpenClover views currently displaying it.")) {
                    model.removeCustomColumn((CustomColumnDefinition) allCustomColumnsTable.getSelection()[0].getData());
                }
            }
        });

        Label separator = new Label(composite, SWT.SEPARATOR | SWT.VERTICAL);
        separator.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        Composite assignedColumns = new Composite(composite, SWT.NONE);
        assignedColumns.setLayout(new GridLayout(3, false));
        assignedColumns.setLayoutData(new GridData(GridData.FILL_BOTH));

        new Label(assignedColumns, SWT.NONE);
        Label assignedLabel = new Label(assignedColumns, SWT.NONE);
        assignedLabel.setText("Assigned columns:");
        SwtUtils.gridDataFor(assignedLabel).horizontalSpan = 2;

        leftButton = new Button(assignedColumns, SWT.NONE);
        leftButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(leftButton).widthHint = BUTTON_WIDTH_HINT;
        leftButton.setText("<");
        leftButton.setEnabled(false);
        leftButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.deassign(
                    (ColumnDefinition)selectedColumnsTable.getItem(selectedColumnsTable.getSelectionIndex()).getData());
            }
        });

        selectedColumnsTable = new Table(assignedColumns, SWT.BORDER);
        selectedColumnsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        SwtUtils.gridDataFor(selectedColumnsTable).verticalSpan = 3;
        SwtUtils.gridDataFor(selectedColumnsTable).heightHint = TABLE_RHS_HEIGHT_HINT;
        SwtUtils.gridDataFor(selectedColumnsTable).widthHint = TABLE_WIDTH_HINT;
        selectedColumnsTableViewer = new TableViewer(selectedColumnsTable);
        selectedColumnsTableViewer.setLabelProvider(new ColumnDefinitionLabelProvider());
        selectedColumnsTableViewer.setContentProvider(new ColumnDefinitionsProvider() {
            @Override
            public Object[] getElements(Object root) {
                return model.getAssigned().toArray();
            }
        });
        selectedColumnsTableViewer.setInput(model);
        selectedColumnsTableViewer.addSelectionChangedListener(event -> updateButtonStates());

        upButton = new Button(assignedColumns, SWT.NONE);
        upButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(upButton).widthHint = BUTTON_WIDTH_HINT;
        upButton.setText("Up");
        upButton.setEnabled(false);
        upButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.moveAssignedUp((ColumnDefinition)selectedColumnsTable.getSelection()[0].getData());
            }
        });

        allLeftButton = new Button(assignedColumns, SWT.NONE);
        allLeftButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(allLeftButton).widthHint = BUTTON_WIDTH_HINT;
        allLeftButton.setText("<<");
        allLeftButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.deassignAll();
            }
        });

        downButton = new Button(assignedColumns, SWT.NONE);
        downButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(downButton).widthHint = BUTTON_WIDTH_HINT;
        downButton.setText("Down");
        downButton.setEnabled(false);
        downButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                model.moveAssignedDown((ColumnDefinition)selectedColumnsTable.getSelection()[0].getData());
            }
        });

        //Filler
        new Label(assignedColumns, SWT.NONE);

        editCustomAssignedButton = new Button(assignedColumns, SWT.NONE);
        editCustomAssignedButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
        SwtUtils.gridDataFor(editCustomAssignedButton).widthHint = BUTTON_WIDTH_HINT;
        editCustomAssignedButton.setText("Edit");
        editCustomAssignedButton.setEnabled(false);
        editCustomAssignedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                CustomColumnDefinition originalDefinition = (CustomColumnDefinition)selectedColumnsTable.getSelection()[0].getData();
                CustomColumnDialog dialog = new CustomColumnDialog(getShell(), model.getBuiltintColumns(), originalDefinition);
                if (dialog.open() == Dialog.OK) {
                    model.replaceCustomColumn(originalDefinition, dialog.getColumn());
                }
            }
        });

        model.addListener(evt -> {
            selectedColumnsTableViewer.refresh();
            allBuiltinColumnsTableViewer.refresh();
            allCustomColumnsTableViewer.refresh();
            updateButtonStates();
        });

        return composite;
    }

    public Set<ColumnDefinition> getCustomColumns() {
        return model.getCustomColumns();
    }

    public List<ColumnDefinition> getVisibleColumns() {
        return model.getAssigned();
    }

    private void doImport() {

    }

    private void doExport() {

    }
    
    private void updateButtonStates() {
        builtinRightButton.setEnabled(allBuiltinColumnsTable.getSelectionIndex() != -1);
        builtinAllRightButton.setEnabled(allBuiltinColumnsTable.getItemCount() > 0);
        customRightButton.setEnabled(allCustomColumnsTable.getSelectionIndex() != -1);
        customAllRightButton.setEnabled(allCustomColumnsTable.getItemCount() > 0);
        editCustomUnassignedButton.setEnabled(allCustomColumnsTable.getSelectionIndex() != -1);
        removeCustomUnassignedButton.setEnabled(allCustomColumnsTable.getSelectionIndex() != -1);
        leftButton.setEnabled(
            selectedColumnsTable.getSelectionIndex() != -1
            && !((ColumnDefinition)selectedColumnsTable.getItem(selectedColumnsTable.getSelectionIndex()).getData()).isLocked());
        allLeftButton.setEnabled(selectedColumnsTable.getItemCount() > 0);
        upButton.setEnabled(
            selectedColumnsTable.getSelectionIndex() > 0
            && !((ColumnDefinition)selectedColumnsTable.getItem(selectedColumnsTable.getSelectionIndex()).getData()).isLocked()
            && !((ColumnDefinition)selectedColumnsTable.getItem(selectedColumnsTable.getSelectionIndex() - 1).getData()).isLocked());
        downButton.setEnabled(
            selectedColumnsTable.getSelectionIndex() >= 0
            && selectedColumnsTable.getSelectionIndex() < selectedColumnsTable.getItemCount() - 1
            && !((ColumnDefinition)selectedColumnsTable.getItem(selectedColumnsTable.getSelectionIndex()).getData()).isLocked()
            && !((ColumnDefinition)selectedColumnsTable.getItem(selectedColumnsTable.getSelectionIndex() + 1).getData()).isLocked());
        editCustomAssignedButton.setEnabled(
            selectedColumnsTable.getSelectionIndex() != -1
            && ((ColumnDefinition)selectedColumnsTable.getItem(selectedColumnsTable.getSelectionIndex()).getData()).isCustom());
    }

    private ColumnDefinition getSelectedDefinition(SelectionChangedEvent event) {
        IStructuredSelection selection =
            event.getSelection() instanceof IStructuredSelection
                ? (IStructuredSelection)event.getSelection()
                : null;

        ColumnDefinition definition =
            selection == null
                ? null
                : (ColumnDefinition)selection.getFirstElement();
        return definition;
    }

}
