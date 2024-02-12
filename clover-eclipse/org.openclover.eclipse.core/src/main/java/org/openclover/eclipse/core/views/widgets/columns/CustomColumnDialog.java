package org.openclover.eclipse.core.views.widgets.columns;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.openclover.core.reporters.ExpressionEvaluator;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.views.BuiltinMetricsColumnDefinition;
import org.openclover.eclipse.core.views.BuiltinPcMetricsColumnDefinition;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.CustomColumnDefinition;
import org.openclover.runtime.api.CloverException;

import java.util.Set;

public class CustomColumnDialog extends TitleAreaDialog {
    private Button validateButton;
    private Text expressionText;
    private Text nameText;
    private CustomColumnDefinition columnDef;
    private Combo formatCombo;
    private Combo alignmentCombo;
    private Button addColumnButton;
    private Set<? extends ColumnDefinition> builtintColumns;

    public CustomColumnDialog(Shell shell, Set<? extends ColumnDefinition> builtintColumns, CustomColumnDefinition customColumnDefinition) {
        this(shell, builtintColumns);
        columnDef = customColumnDefinition;
    }

    public CustomColumnDialog(Shell shell, Set<? extends ColumnDefinition> builtintColumns) {
        super(shell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
        this.builtintColumns = builtintColumns;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Custom Column Builder");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Define a custom column using the Clover expression language");
        setMessage("Enter the column name, alignment, format and expression.");

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(3, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.gridDataFor(separator).horizontalSpan = 3;

        Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.gridDataFor(nameLabel).horizontalSpan = 3;
        nameLabel.setText("Name:");

        nameText = new Text(composite, SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameText.setText(columnDef == null ? "" : columnDef.getTitle());
        SwtUtils.gridDataFor(nameText).horizontalSpan = 2;

        //Filler
        new Label(composite, SWT.NONE);

        Label alignmentLabel = new Label(composite, SWT.NONE);
        alignmentLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        alignmentLabel.setText("Aligment:");

        Label formatLabel = new Label(composite, SWT.NONE);
        formatLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        formatLabel.setText("Format:");

        //Filler
        new Label(composite, SWT.NONE);

        alignmentCombo = new Combo(composite, SWT.READ_ONLY);
        alignmentCombo.add("Left");
        alignmentCombo.add("Center");
        alignmentCombo.add("Right");
        alignmentCombo.select(
            columnDef == null
                ? 0
                : columnDef.getAlignment() == SWT.LEFT
                    ? 0
                    : columnDef.getAlignment() == SWT.CENTER
                        ? 1
                        : 2);

        formatCombo = new Combo(composite, SWT.READ_ONLY);
        formatCombo.add("Raw value");
        formatCombo.add("Percentage");
        formatCombo.select(
            columnDef == null
                ? 0
                : columnDef.getFormat() == CustomColumnDefinition.RAW_FORMAT
                    ? 0
                    : 1);

        //Filler
        new Label(composite, SWT.NONE);

        Label expressionLabel = new Label(composite, SWT.NONE);
        expressionLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.gridDataFor(expressionLabel).horizontalSpan = 2;
        expressionLabel.setText("Expression:");

        expressionText = new Text(composite, SWT.BORDER | SWT.MULTI);
        expressionText.setLayoutData(new GridData(GridData.FILL_BOTH));
        SwtUtils.gridDataFor(expressionText).horizontalSpan = 2;
        SwtUtils.gridDataFor(expressionText).verticalSpan = 2;
        expressionText.setFont(new Font(null, "Courier", 14, SWT.NONE));
        expressionText.setText(columnDef == null ? "" : columnDef.getExpression());
        SwtUtils.gridDataFor(expressionText).heightHint = 300;
        SwtUtils.gridDataFor(expressionText).widthHint = 350;

        validateButton = new Button(composite, SWT.NONE);
        validateButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        validateButton.setText("Validate");
        validateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    ExpressionEvaluator.validate(expressionText.getText(), nameText.getText());
                    setMessage("Expression is valid.", IMessageProvider.INFORMATION);
                } catch (CloverException e) {
                    setMessage("Expression is invalid: " + e.getMessage(), IMessageProvider.ERROR);
                }
            }
        });

        addColumnButton = new Button(composite, SWT.PUSH | SWT.ARROW);
        addColumnButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        addColumnButton.setText("Insert...");

        MenuManager menuManager = new MenuManager();
        final Menu menu = menuManager.createContextMenu(addColumnButton);
        for (ColumnDefinition columnDef : builtintColumns) {
            if (columnDef instanceof BuiltinMetricsColumnDefinition) {
                final BuiltinMetricsColumnDefinition metricsColDef = (BuiltinMetricsColumnDefinition) columnDef;
                menuManager.add(new Action(columnDef.getTitle() + "\t(" + metricsColDef.getPrototype().getName() + ")") {
                    @Override
                    public void run() {
                        int caretPos = expressionText.getCaretPosition();
                        String charBefore = expressionText.getText(caretPos - 1, caretPos);
                        String charAfter = expressionText.getText(caretPos, caretPos + 1);
                        expressionText.insert(
                                (charBefore.trim().length() == 0 ? "" : " ")
                                        + (metricsColDef instanceof BuiltinPcMetricsColumnDefinition ? "%" : "")
                                        + metricsColDef.getPrototype().getName()
                                        + (charAfter.trim().length() == 0 ? "" : " "));
                    }
                });
            }
        }
        addColumnButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Point buttonLocation = addColumnButton.getParent().toDisplay(addColumnButton.getLocation());
                Rectangle buttonBounds = addColumnButton.getBounds();
                menu.setLocation(buttonLocation.x, buttonLocation.y + buttonBounds.height);
                menu.setVisible(true);
            }
        });

        return composite;
    }

    @Override
    protected void okPressed() {
        if (nameText.getText().trim().length() == 0) {
            setMessage("The column title must be non-blank.", IMessageProvider.ERROR);
            return;
        }

        if (expressionText.getText().trim().length() == 0) {
            setMessage("The column expression must be non-blank.", IMessageProvider.ERROR);
            return;
        } else {
            try {
                int alignmentSelection = alignmentCombo.getSelectionIndex();
                int alignment =
                    alignmentSelection == 0
                        ? SWT.LEFT
                        : alignmentSelection == 1
                            ? SWT.CENTER
                            : SWT.RIGHT;
                int formatSeletion = formatCombo.getSelectionIndex();
                int format =
                    formatSeletion == 0
                        ? CustomColumnDefinition.RAW_FORMAT
                        : CustomColumnDefinition.PC_FORMAT;

                if (columnDef == null) {
                    columnDef = new CustomColumnDefinition(
                        nameText.getText(),
                        nameText.getText(),
                        expressionText.getText(),
                        alignment,
                        format);
                } else {
                    columnDef = new CustomColumnDefinition(
                        columnDef.getId(),
                        nameText.getText(),
                        nameText.getText(),
                        expressionText.getText(),
                        alignment,
                        format);
                }
            } catch (CloverException e) {
                setMessage("The column expression contains errors: " + e.getMessage(), IMessageProvider.ERROR);
                return;
            }
        }

        super.okPressed();
    }

    @Override
    public boolean close() {
        return super.close();

    }

    public CustomColumnDefinition getColumn() {
        return columnDef;
    }
}
