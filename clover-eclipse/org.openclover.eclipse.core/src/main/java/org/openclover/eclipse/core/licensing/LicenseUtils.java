package org.openclover.eclipse.core.licensing;

import com.atlassian.clover.CloverLicenseInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;


import com_atlassian_clover.CloverVersionInfo;
import clover.org.apache.commons.codec.binary.Base64;

public class LicenseUtils {
    private static final String DEFAULT_DISABLED_LICENSE_TEXT = "Your license has been disabled";
    private static final String DEFAULT_EXPIRED_LICENSE_TEXT = "Your license has expired";
    private static final String PREFS_TRAIL_LINK = "Window>Preferences>Clover>License";
    private static final String GENERATE_LICENSE_LINK_TITLE = "generate an evaluation license";
    private static final String MY_LICENSE_LINK_TITLE = "retrieve your existing license";
    public static final String LICENSE_TOKEN_PROP = "clover.eclipse.license.token";

    public static void openLicensePreferencesPage(Shell shell) {
        PreferencesUtil.createPreferenceDialogOn(
            shell,
            "org.openclover.eclipse.core.preferences.cloverLicensePreferencesPage",
            null,
            null).open();
    }

    public static String calcExpiryStatement() {
        if (CloverLicenseInfo.POST_EXPIRY_STMT == null) {
            return DEFAULT_EXPIRED_LICENSE_TEXT;
        } else {
            return CloverLicenseInfo.POST_EXPIRY_STMT;
        }
    }

    public static String calcExtendedExpiryStatement(boolean asLink) {
        return
            endInPeriod(calcExpiryStatement())
            + "\n\n" + calcLicenseNextStepsStatement(asLink);
    }

    public static String endInPeriod(String string) {
        return (string.lastIndexOf('.') == string.length() - 1) ? string : string + ".";
    }

    public static String calcLicenseNextStepsStatement(boolean asLink) {
            return
                calcLicenseLinkStatment(asLink)
                + " Once you have a valid license file you can install this in "
                + openLink(asLink) + PREFS_TRAIL_LINK + closeLink(asLink);
    }

    public static String calcLicenseLinkStatment(boolean asLink) {
        return
            "You can " + openLink(asLink) + GENERATE_LICENSE_LINK_TITLE + closeLink(asLink)
            + " (registration required)"
            + " or you can " + openLink(asLink) + MY_LICENSE_LINK_TITLE + closeLink(asLink) + " on the website.";
    }

    private static String openLink(boolean asLink) {
        return asLink ? "<a>" : "";
    }

    private static String closeLink(boolean asLink) {
        return asLink ? "</a>" : "";
    }

    public static String calcTerminationStatement() {
        if (CloverLicenseInfo.TERMINATION_STMT == null) {
            if (CloverLicenseInfo.EXPIRED && CloverLicenseInfo.POST_EXPIRY_STMT == null) {
                //Just in case...
                return DEFAULT_DISABLED_LICENSE_TEXT;
            } else {
                //If termination and expiry set to same date, termination statement will be
                //null but TERMINATED will be true
                return CloverLicenseInfo.POST_EXPIRY_STMT;
            }
        } else {
            return CloverLicenseInfo.TERMINATION_STMT;
        }
    }

    public static String calcExtendedTerminationStatement(boolean asLink) {
        return
            endInPeriod(calcTerminationStatement())
            + "\n\n" + calcLicenseNextStepsStatement(asLink);
    }

    public static boolean isPreferencesLink(String link) {
        return PREFS_TRAIL_LINK.equals(link);
    }

    public static String calcInstallDateToken() {
        final long installDate = CloverPlugin.getInstance().getInstallationSettings().getInstallDate();
        return new String(Base64.encodeBase64( (CloverVersionInfo.SANITIZED_RN + ":" + installDate).getBytes(StandardCharsets.US_ASCII) ));
    }
    
    public static long parseInstallDateToken(String licenseToken) {
        long installDate = 0l;
        try {
            if (licenseToken != null) {
                final String[] tokenParts = new String(Base64.decodeBase64(licenseToken.getBytes(StandardCharsets.US_ASCII)), StandardCharsets.US_ASCII).split(":");
                if (tokenParts.length == 2 && CloverVersionInfo.SANITIZED_RN.equals(tokenParts[0])) {
                    return Long.parseLong(tokenParts[1]);
                }
            }
        } catch (Exception e) {
            CloverPlugin.logWarning("Failed to determine install date from " + licenseToken, e);
        }
        return installDate;
    }

    public static boolean writeLicenseTo(File licenseFile) throws IOException {
        String licenseText =
            CloverPlugin.getInstance().getInstallationSettings().getLicenseText();

        if (licenseFile != null) {
            if (!licenseFile.exists()) {
                licenseFile.createNewFile();
            }
            FileWriter writer = new FileWriter(licenseFile);
            writer.write(licenseText);
            writer.flush();
            writer.close();
            return true;
        } else {
            return false;
        }
    }
}
