package org.openclover.eclipse.core.reports;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

import java.io.File;

public abstract class OpenReportOperation {

    public abstract String getName();

    public abstract boolean supports(ReportHistoryEntry report);

    public abstract void open(ReportHistoryEntry entry);

    protected IWorkbenchPage getActivePage() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }

    protected IEditorInput inputFor(String path) {
        return new ReportEditorInput(new File(path));
    }
}
