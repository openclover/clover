package com.atlassian.clover;

public final class CloverLicense {

    public static final long ONE_DAY = 24L * 60L * 60L * 1000L;

    private final String productName;
    private final String licenseName;
    private final String ownerName;
    private final long licenseExpiryDate;
    private final long maintenanceExpiryDate;

    CloverLicense(String productName,
                  String licenseName,
                  String ownerName,
                  long licenseExpiryDate,
                  long maintenanceExpiryDate) {
        this.productName = productName;
        this.licenseName = licenseName;
        this.ownerName = ownerName;
        this.licenseExpiryDate = licenseExpiryDate;
        this.maintenanceExpiryDate = maintenanceExpiryDate;
    }

    public String getProductName() {
        return productName;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getOrganisation() {
        return ownerName;
    }

    public long getLicenseExpiryDate() {
        return licenseExpiryDate;
    }

    public long getMaintenanceExpiryDate() {
        return maintenanceExpiryDate;
    }

    public boolean expires() {
        return licenseExpiryDate != 0;
    }

    public boolean maintenanceExpires() {
        return maintenanceExpiryDate != 0;
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isMaintenanceExpired() {
        return isMaintenanceExpired(System.currentTimeMillis());
    }

    public String getOwnerStatement() {
        return productName + " is " + licenseName + " software.";
    }

    public String getPreExpiryStatement() {
        return "";
    }

    public String getPostExpiryStatement() {
        return "";
    }

    public String getContactInfoStatement() {
        return "";
    }

    public long getDaysTillExpiry() {
        return getDaysTillExpiry(System.currentTimeMillis());
    }

    private boolean isExpired(long time) {
        return expires() && time > licenseExpiryDate;
    }

    private boolean isMaintenanceExpired(long time) {
        return maintenanceExpires() && time > maintenanceExpiryDate;
    }

    private long getDaysTillExpiry(long now) {
        long days = countDays(licenseExpiryDate - now);
        return (days < 0) ? 0 : days;
    }

    private long countDays(long aMilliseconds) {
        return aMilliseconds / (ONE_DAY);
    }
}
