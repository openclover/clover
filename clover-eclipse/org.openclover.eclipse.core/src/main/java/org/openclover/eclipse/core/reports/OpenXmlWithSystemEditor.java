package org.openclover.eclipse.core.reports;

import org.openclover.core.reporters.Type;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

class OpenXmlWithSystemEditor extends OpenReportWithSystemEditor {

    @Override
    public String getName() {
        return CloverEclipsePluginMessages.SYSTEM_XML_VIEWER();
    }

    @Override
    public boolean supports(ReportHistoryEntry report) {
        return report.getType() == Type.XML;
    }
}
