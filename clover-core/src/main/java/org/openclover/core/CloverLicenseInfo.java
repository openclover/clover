package org.openclover.core;

public class CloverLicenseInfo {

    /**
     * Expired indicates whether or not this license has expired. An expired
     * license will still activate Clover.
     */
    public static boolean EXPIRED = false;

    /**
     * Terminated indicates whether or not this license has terminated. A
     * terminated license will disable Clover.
     */
    public static boolean TERMINATED = false;

    /**
     * Expires is true if this license has an expiry time, false otherwise.
     */
    public static boolean EXPIRES = false;

    public static String NAME;
    public static String OWNER_STMT;
    public static String PRE_EXPIRY_STMT;
    public static String POST_EXPIRY_STMT;
    public static String CONTACT_INFO_STMT;
    public static String TERMINATION_STMT;
    public static long DAYS_REMAINING;
}
