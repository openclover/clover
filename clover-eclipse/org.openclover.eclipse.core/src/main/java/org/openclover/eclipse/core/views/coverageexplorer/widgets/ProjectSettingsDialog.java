package org.openclover.eclipse.core.views.coverageexplorer.widgets;

import org.openclover.core.cfg.instr.InstrumentationLevel;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ProjectSettingsDialog extends PopupDialog {
    private final CloverProject project;
    private final Point location;
    private final boolean initialCompileWithCloverSetting;
    private final InstrumentationLevel initialInstrumentationlevel;

    private Button apply;
    private Button compileWithClover;
    private Combo instrumentationLevel;

    public ProjectSettingsDialog(Shell parent, CloverProject project, Point location) {
        super(parent, INFOPOPUP_SHELLSTYLE, true, false, false, false, false,
                String.format("Clover settings for %s", project.getName()), null);
        this.project = project;
        this.location = location;
        this.initialCompileWithCloverSetting = project.getSettings().isInstrumentationEnabled();
        this.initialInstrumentationlevel = project.getSettings().getInstrumentationLevel();
    }

    @Override
    protected Control createDialogArea(Composite composite) {
        Composite parent = new Composite(composite, SWT.NONE);
        parent.setLayout(new GridLayout(3, false));

        SelectionListener onChangeEnableApply = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                apply.setEnabled(true);
            }
        };

        compileWithClover = new Button(parent, SWT.CHECK);
        compileWithClover.setText("Instrument and compile at ");
        compileWithClover.setToolTipText(
            "Choose whether Clover should instrument and compile your source code and what coverage granularity.\n\n" +
            "This can be switched off if you wish to develop without tracking code coverage for a period of time or " +
            "if you have configured Clover-for-Ant or Clover-for-Maven to instrument and compile your source for you."
        );
        compileWithClover.setSelection(initialCompileWithCloverSetting);
        compileWithClover.addSelectionListener(onChangeEnableApply);
        compileWithClover.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                instrumentationLevel.setEnabled(compileWithClover.getSelection());
            }
        });

        instrumentationLevel = new Combo(parent, SWT.CHECK | SWT.READ_ONLY);
        SwtUtils.gridDataFor(instrumentationLevel).horizontalSpan = 2;
        instrumentationLevel.setItems(new String[] {"statement level", "method level"});
        instrumentationLevel.select(initialInstrumentationlevel == InstrumentationLevel.STATEMENT ? 0 : 1);
        instrumentationLevel.setToolTipText(
            "Statement level instrumentation is more accurate but has a runtime performance penalty." +
            "Method level instrumentation is less accurate but will run faster and Clover will be able to provide coverage feedback more switfly.\n\n" +
            "If you only use Clover for optimizing your test runs, method level instrumenation is the best option.");
        instrumentationLevel.addSelectionListener(onChangeEnableApply);

        apply = new Button(parent, SWT.NONE);
        apply.setText("Apply");
        SwtUtils.gridDataFor(apply).horizontalSpan = 3;
        SwtUtils.gridDataFor(apply).horizontalAlignment = GridData.END;
        apply.setEnabled(false);
        apply.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                final boolean newCompileWithCloverSetting =
                    compileWithClover.getSelection();
                final InstrumentationLevel newInstrementationlevel =
                    instrumentationLevel.getSelectionIndex() == 0
                        ? InstrumentationLevel.STATEMENT
                        : InstrumentationLevel.METHOD;
                project.getSettings().setInstrumentationEnabled(newCompileWithCloverSetting);
                project.getSettings().setInstrumentationLevel(newInstrementationlevel);
                close();
                if ((newCompileWithCloverSetting != initialCompileWithCloverSetting
                    || newInstrementationlevel != initialInstrumentationlevel)
                    && project.okayToRebuild(getParentShell())) {
                    try {
                        project.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
                    } catch (CoreException e) {
                        CloverPlugin.logError("Failed to rebuild project after changing 'compile with clover' state", e);
                    }
                }
            }
        });

        return parent;
    }

    @Override
    protected Point getInitialLocation(Point point) {
        return location;
    }
}
