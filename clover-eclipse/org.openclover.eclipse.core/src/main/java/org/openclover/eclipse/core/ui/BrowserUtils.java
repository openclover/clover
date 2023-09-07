package org.openclover.eclipse.core.ui;

import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.PlatformUI;

import java.net.URL;

import org.openclover.eclipse.core.CloverPlugin;

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
            CloverPlugin.logError("Unable to launch external browser", e);
        }
    }
}
