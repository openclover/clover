package org.openclover.eclipse.core.ui;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.openclover.eclipse.core.CloverPlugin;

import java.net.URL;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class BrowserUtils {
    public static void openExternalBrowser(String url) {
        IWorkbenchBrowserSupport browserSupport =
            PlatformUI.getWorkbench().getBrowserSupport();
        try {
            IWebBrowser browser = browserSupport.getExternalBrowser();
            if (browser != null) {
                browser.openURL(new URL(url));
            }
        } catch (Exception e) {
            logError("Unable to launch external browser", e);
        }
    }
}
