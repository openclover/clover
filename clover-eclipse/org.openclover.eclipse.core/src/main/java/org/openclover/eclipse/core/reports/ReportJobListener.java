package org.openclover.eclipse.core.reports;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.jface.dialogs.MessageDialog;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

import java.util.List;

/**
 *
 */
public class ReportJobListener extends JobChangeAdapter {
    protected final Shell shell;

    public ReportJobListener(Shell shell) {
        this.shell = shell;
    }

    @Override
    public void done(final IJobChangeEvent event) {
        Display.getDefault().syncExec(
            new Runnable() {
                @Override
                public void run() {
                    if (event.getResult().getSeverity() == Status.OK || event.getResult().getSeverity() == Status.WARNING) {
                        CloverPlugin.getInstance().addReportToHistory(
                            new ReportHistoryEntry(
                                ((ReportJob)event.getJob()).getConfig(),
                                System.currentTimeMillis()));
                        onReportSucceeded(event);
                    } else if (event.getResult().getSeverity() == Status.ERROR) {
                        onReportFailed(event);
                    } else {
                        onReportCancelled(event);
                    }
                }
            });
    }

    private void onReportCancelled(IJobChangeEvent event) {
        MessageDialog.openWarning(
            shell,
            CloverEclipsePluginMessages.REPORT_MESSAGEBOX_TITLE(((ReportJob) event.getJob()).getConfig().getTitle()),
            CloverEclipsePluginMessages.REPORT_CANCEL_MESSAGE());
    }

    private void onReportFailed(IJobChangeEvent event) {
        Throwable exception = event.getResult().getException();
        String message = event.getResult().getMessage();
        MessageDialog.openError(
            shell,
            CloverEclipsePluginMessages.REPORT_MESSAGEBOX_TITLE(((ReportJob) event.getJob()).getConfig().getTitle()),
            message == null
                ? (exception == null ? "Unknown exception class" : exception.getClass().getName())
                    + ": "
                    + (exception == null ? "unknown error message" : exception.getMessage())
                : message);
    }

    protected void onReportSucceeded(IJobChangeEvent event) {
        ReportHistoryEntry report =
            new ReportHistoryEntry(
                ((ReportJob)event.getJob()).getConfig(),
                System.currentTimeMillis());

        List availableMethods = OpenReportOperation.findFor(report);

        OpenReportOperation chosenMethod =
            OpenReportDialog.openOnGenerate(
                shell,
                report,
                availableMethods);

        if (chosenMethod != null) {
            chosenMethod.open(report);
        }
    }
}
