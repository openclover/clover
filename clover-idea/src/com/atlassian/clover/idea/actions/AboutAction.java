package com.atlassian.clover.idea.actions;

import com.atlassian.clover.CloverLicense;
import com.atlassian.clover.CloverLicenseInfo;
import com.atlassian.clover.CloverStartup;
import com.atlassian.clover.idea.AboutDialog;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.CloverPlugin;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;

import javax.swing.Icon;

public class AboutAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        new AboutDialog(DataKeys.PROJECT.getData(e.getDataContext())).show();
    }

    @Override
    public void update(AnActionEvent e) {
        final CloverLicense license = CloverPlugin.getPlugin().getLicense();
        final boolean maintenanceExpired = license != null && license.isMaintenanceExpired();
        final boolean licenseExpiresSoon = CloverLicenseInfo.EXPIRES && CloverLicenseInfo.DAYS_REMAINING < 30;

        final Icon icon;
        if (maintenanceExpired) {
            icon = CloverIcons.ABOUT_MAINT_EXPIRED;
        } else if (CloverLicenseInfo.TERMINATED || CloverLicenseInfo.EXPIRED || licenseExpiresSoon) {
            // non-eval license expiring is a serious thing
            icon = CloverIcons.ABOUT_EXPIRED;
        } else {
            icon = CloverIcons.ABOUT;
        }
        final Presentation presentation = e.getPresentation();
        if (presentation.getIcon() != icon) {
            presentation.setIcon(icon);
        }

        final String actionText;
        if (CloverLicenseInfo.TERMINATED) {
            actionText = CloverLicenseInfo.TERMINATION_STMT;
        } else if (CloverLicenseInfo.EXPIRED) {
            actionText = "<html>" + CloverLicenseInfo.OWNER_STMT + "<br>" + CloverLicenseInfo.POST_EXPIRY_STMT;
        } else if (licenseExpiresSoon) {
            actionText = "<html>" + CloverLicenseInfo.OWNER_STMT + "<br>" + CloverLicenseInfo.PRE_EXPIRY_STMT;
        } else if (maintenanceExpired) {
            actionText = "<html>" + CloverLicenseInfo.OWNER_STMT + "<br>" + "Clover maintenance period expired";
        } else {
            actionText = getTemplatePresentation().getText();
        }
        presentation.setText(actionText);
    }
}
