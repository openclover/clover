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

class OpenHtmlWithEclipseBrowser extends OpenReportOperation {

    @Override
    public String getName() {
        return CloverEclipsePluginMessages.ECLIPSE_BROWSER();
    }

    @Override
    public boolean supports(ReportHistoryEntry report) {
        return report.getType() == Type.HTML;
    }

    @Override
    public void open(ReportHistoryEntry entry) {
        try {
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser = browserSupport.createBrowser("OpenClover HTML Report Browser");
            if (browser != null) {
                browser.openURL(new File(entry.getPath()).toURI().toURL());
            } else {
                logWarning("No Eclipse HTML browser");
            }
        } catch (Exception e) {
            logError("Unable to open Eclipse HTML browser", e);
        }
    }
}
