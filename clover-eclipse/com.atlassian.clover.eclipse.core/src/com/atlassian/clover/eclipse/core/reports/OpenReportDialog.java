package com.atlassian.clover.eclipse.core.reports;

import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.reports.model.ReportHistoryEntry;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;

import java.util.List;

public class OpenReportDialog extends MessageDialog {
    private final List<OpenReportOperation> reportOpenMethods;
    private OpenReportOperation selectedOpenMethod;
    private Combo openActionCombo;

    public OpenReportDialog(Shell parentShell, String reportTitle, List<OpenReportOperation> reportOpenMethods, String openMessage, String[] buttons) {
        super(
            parentShell,
            CloverEclipsePluginMessages.REPORT_MESSAGEBOX_TITLE(reportTitle),
            null,
            openMessage,
            INFORMATION,
            buttons,
            0);
        this.reportOpenMethods = reportOpenMethods;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        createMessageArea(parent);
        //Padding
        new Label(parent, SWT.NONE);
        //Custom area, we manage this ourselves
        Composite customArea = new Composite(parent, SWT.NONE);
        customArea.setLayout(new GridLayout(2, false));

        openActionCombo = new Combo(customArea, SWT.READ_ONLY);
        for (OpenReportOperation method : reportOpenMethods) {
            openActionCombo.add(method.getName());
        }
        openActionCombo.select(0);

        return customArea;
    }

    @Override
    public boolean close() {
        if (getReturnCode() == MessageDialog.OK) {
            selectedOpenMethod = reportOpenMethods.get(openActionCombo.getSelectionIndex());
        }
        return super.close();
    }

    public OpenReportOperation getSelectedOpenMethod() {
        return selectedOpenMethod;
    }

    public static OpenReportOperation openOnRevist(Shell shell, ReportHistoryEntry entry, List<OpenReportOperation> reportOpenMethods) {
        OpenReportDialog dialog =
            new OpenReportDialog(
                shell,
                entry.getName(),
                reportOpenMethods,
                CloverEclipsePluginMessages.REPORT_OPEN_IN(),
                new String[]{IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL});
        dialog.open();
        return dialog.getSelectedOpenMethod();
    }

    public static OpenReportOperation openOnGenerate(Shell shell, ReportHistoryEntry entry, List<OpenReportOperation> reportOpenMethods) {
        OpenReportDialog dialog =
            new OpenReportDialog(
                shell,
                entry.getName(),
                reportOpenMethods,
                CloverEclipsePluginMessages.REPORT_SUCCESS_MESSAGE(),
                new String[]{"Open", IDialogConstants.CANCEL_LABEL});
        dialog.open();
        return dialog.getSelectedOpenMethod();
    }
}
