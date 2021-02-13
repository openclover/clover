package com.atlassian.clover.eclipse.core.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import java.net.URL;

import com.atlassian.clover.eclipse.core.CloverPlugin;

public final class CloverPluginIcons {
    public static final String PROJECT_REFRESH_ICON = "icons/clcl16/project_refresh.gif";
    public static final String REPORT_WIZARD_ICON = "icons/wizban/report.gif";
    public static final String REPORT_ICON = "icons/clcl16/report.gif";
    public static final String CLOVER_LOGO_ICON = "icons/logo.png";

    public static final String TEST_ICON = "icons/obj16/test.gif";
    public static final String TEST_PASS_ICON = "icons/obj16/testpass.gif";
    public static final String TEST_ERROR_ICON = "icons/obj16/testerror.gif";
    public static final String TEST_FAILURE_ICON = "icons/obj16/testfailure.gif";
    public static final String TEST_CLASS_ICON = "icons/obj16/class.gif";

    public static final String ZOOMOUT = "icons/clcl16/zoomout.gif";
    public static final String ZOOMIN = "icons/clcl16/zoomin.gif";

    public static final String CLOVERED_OVERLAY_ICON = "icons/ovr16/clovered.gif";
    public static final String CLOVERED_NO_COMPILE_OVERLAY_ICON = "icons/ovr16/clovered_nocompile.gif";
    public static final String CLOVERED_ERROR_OVERLAY_ICON = "icons/ovr16/clovered_error.gif";
    public static final String LOCKED_OVERLAY_ICON = "icons/ovr16/locked.gif";

    public static final String CLOVER_OVL_GRAY_ICON = "icons/ovr16/clover_ovl_gray.gif";
    public static final String CLOVER_OVL_GREEN_ICON = "icons/ovr16/clover_ovl_green.gif";
    public static final String CLOVER_OVL_RED_ICON = "icons/ovr16/clover_ovl_red.gif";

    public static Image grabPluginImage(ResourceManager imageManager, String bundleName, String path) {
        Bundle bundle = Platform.getBundle(bundleName);
        if (bundle != null) {
            URL resourceURL = bundle.getResource(path);
            if (resourceURL != null) {
                try {
                    return imageManager.createImage(ImageDescriptor.createFromURL(resourceURL));
                } catch (Exception e) {
                    CloverPlugin.logError("Failed to create image '" + path + "' for bundle '" + bundleName + "'", e);
                }
            }
        }
        return null;
    }
}
