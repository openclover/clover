package org.openclover.core;

import org.openclover.core.util.ClassPathUtil;
import org.openclover.core.util.format.ConsoleFormatter;
import org.openclover.runtime.Logger;
import org_openclover_runtime.CloverVersionInfo;

public class CloverStartup {

    public static void loadLicense(Logger log) {
        loadLicense(log, true, 0);
    }

    public static void loadLicense(Logger log, boolean fail) {
        loadLicense(log, fail, 0);
    }

    /**
     * Load the Clover license and configure the CloverLicenseInfo object.
     *
     * @param log where to write messages
     * @param fail if true, throw runtime exceptions, otherwise just return
     * @param pluginInstallTs if being loaded by an IDE plugin, then the ts the plugin was installed
     *
     * @throws RuntimeException if the license has reached its hard expiry time.
     */
    public static void loadLicense(Logger log, boolean fail, long pluginInstallTs) {

        logVersionInfo(log);

        try {
            final CloverLicense license = CloverLicenseDecoder.decode("");

            CloverLicenseInfo.NAME = license.getLicenseName();
            CloverLicenseInfo.OWNER_STMT = license.getOwnerStatement();
            CloverLicenseInfo.CONTACT_INFO_STMT = license.getContactInfoStatement();
            CloverLicenseInfo.PRE_EXPIRY_STMT = license.getPreExpiryStatement();
            CloverLicenseInfo.POST_EXPIRY_STMT = license.getPostExpiryStatement();
            CloverLicenseInfo.EXPIRED = license.isExpired();
            CloverLicenseInfo.EXPIRES = license.expires();

            // do one last check for the daysleft string.
            CloverLicenseInfo.DAYS_REMAINING = license.getDaysTillExpiry();
            evaluateDaysLeft(CloverLicenseInfo.DAYS_REMAINING);

            log.debug(ConsoleFormatter.format(CloverLicenseInfo.OWNER_STMT));
        } catch (LicenseDecoderException e) {
            log.debug(e);
        }
    }

    public static void logVersionInfo(Logger log) {
        log.info("OpenClover Version " + CloverVersionInfo.RELEASE_NUM +
                ", built on " + CloverVersionInfo.BUILD_DATE);
        String loadedFrom =  ClassPathUtil.getCloverJarPath();
        if (loadedFrom != null) {
            log.debug("Loaded from: " + loadedFrom);
        } else {
            log.debug("Couldn't determine path we were loaded from.");
        }
    }

    /**
     * The License Loader interface provides a hook through which custom
     * license loaders can be implemented.
     * <p>One benefit is that this will allow unit tests to specify and change
     * there own licenses at runtime.
     */
    public interface LicenseLoader {
        String LICENSE_FILE_NAME = "clover.license";
        CloverLicense loadLicense(Logger log);
    }

    /**
     * Set the CloverStartup license loader. The specified license loader will
     * be used by the loadLicense methods to retrieve the license information
     * and configure the systems license information.
     *
     * @see CloverStartup#loadLicense(Logger)
     * @see CloverStartup#loadLicense(Logger, boolean)
     */
    public static void setLicenseLoader(LicenseLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("CloverStartup does not support a 'null' LicenseLoader.");
        }
    }

    /**
     * One final post processing run to remove the '$daysleft string from
     * the license messages, replacing them with the provided value.
     */
    private static void evaluateDaysLeft(long daysLeft) {
        final String regex = "\\$[dD][aA][yY][sS][lL][eE][fF][tT]"; // $daysleft
        if (daysLeft < 0) {
            daysLeft = 0;
        }
        CloverLicenseInfo.PRE_EXPIRY_STMT =
                CloverLicenseInfo.PRE_EXPIRY_STMT.replaceAll(regex, Long.toString(daysLeft));
    }
}