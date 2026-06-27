package org.openclover.eclipse.core.reports;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.openclover.core.reporters.Type;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

import java.io.File;

import static org.openclover.eclipse.core.CloverPlugin.logError;
import static org.openclover.eclipse.core.CloverPlugin.logWarning;

class OpenHtmlWithSystemBrowser extends OpenReportOperation {

    @Override
    public String getName() {
        return CloverEclipsePluginMessages.SYSTEM_BROWSER();
    }

    @Override
    public boolean supports(ReportHistoryEntry report) {
        return report.getType() == Type.HTML
            && PlatformUI.getWorkbench().getBrowserSupport().isInternalWebBrowserAvailable();
    }

    @Override
    public void open(ReportHistoryEntry entry) {
        try {
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser = browserSupport.getExternalBrowser();
            if (browser != null) {
                browser.openURL(new File(entry.getPath()).toURI().toURL());
            } else {
                logWarning("No system HTML browser");
            }
        } catch (Exception e) {
            logError("Unable to launch system HTML browser", e);
        }
    }
}
