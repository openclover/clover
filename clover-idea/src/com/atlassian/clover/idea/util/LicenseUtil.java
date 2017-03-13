package com.atlassian.clover.idea.util;

import com.atlassian.clover.CloverLicense;
import com_atlassian_clover.CloverVersionInfo;

public class LicenseUtil {
    private static final String DEFAULT_EXPIRED_LICENSE_TEXT = "Your license has expired";
    public static final String GENERATE_LICENSE_LINK_TITLE = "generate an evaluation license";
    public static final String MY_LICENSE_LINK_TITLE = "retrieve your existing license";

    public static String calcExpiryStatement(CloverLicense license) {
        if (license.getPostExpiryStatement() == null) {
            return DEFAULT_EXPIRED_LICENSE_TEXT;
        } else {
            return license.getPostExpiryStatement();
        }
    }

    public static String getGenerateLicenseLink() {
        return CloverVersionInfo.CLOVER_GENERATE_LICENSE_URL;
    }

    public static String getMyLicenseLink() {
        return CloverVersionInfo.CLOVER_LICENSE_URL;
    }
}