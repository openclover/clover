package org.openclover.eclipse.core.reports;

import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ide.IDE;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

import static org.openclover.eclipse.core.CloverPlugin.logError;

abstract class OpenReportWithSystemEditor extends OpenReportOperation {

    @Override
    public void open(ReportHistoryEntry entry) {
        try {
            IDE.openEditor(getActivePage(), inputFor(entry.getPath()), IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
        } catch (Exception e) {
            logError("Unable to launch system reader for " + entry.getPath(), e);
        }
    }
}
