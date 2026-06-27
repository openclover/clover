package org.openclover.eclipse.core.reports;

import org.eclipse.ui.ide.IDE;
import org.openclover.core.reporters.Type;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class OpenWithEditor extends OpenReportOperation {

    private final String editorId;
    private final Type type;
    private final String name;

    public OpenWithEditor(String editorId, Type type, String name) {
        this.editorId = editorId;
        this.type = type;
        this.name = name;
    }

    @Override
    public String getName() {
        return CloverEclipsePluginMessages.ECLIPSE_VIEWER(name);
    }

    @Override
    public boolean supports(ReportHistoryEntry report) {
        return type == report.getType();
    }

    @Override
    public void open(ReportHistoryEntry entry) {
        try {
            IDE.openEditor(getActivePage(), inputFor(entry.getPath()), editorId);
        } catch (Exception e) {
            logError("Unable to launch Eclipse editor " + editorId, e);
        }
    }
}
