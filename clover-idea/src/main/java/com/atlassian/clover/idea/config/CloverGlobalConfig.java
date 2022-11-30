package com.atlassian.clover.idea.config;

/**
 * Clover configuration stored globally (in the IDEA installation directory). IDEA's bean serialization mechanism is
 * used (see {@link com.intellij.openapi.components.PersistentStateComponent} and {@link com.intellij.util.xmlb.BeanBinding}).
 *
 * @see com.atlassian.clover.idea.CloverPlugin
 */
public class CloverGlobalConfig {

    private static final long DATE_UNDEFINED = -1L;
    private String licenseText = "";
    private long installDate = DATE_UNDEFINED;

    public CloverGlobalConfig() {

    }

    public CloverGlobalConfig(String licenseText, long installDate) {
        this.licenseText = (licenseText != null ? licenseText : "");
        this.installDate = installDate;
    }

    public String getLicenseText() {
        return licenseText;
    }

    @SuppressWarnings("unused") // used for bean<->xml serialization
    public void setLicenseText(String licenseText) {
        this.licenseText = (licenseText != null ? licenseText : "");
    }

    public long getInstallDate() {
        return installDate;
    }

    @SuppressWarnings("unused") // used for bean<->xml serialization
    public void setInstallDate(long installDate) {
        this.installDate = installDate;
    }

    public boolean isInstallDateSet() {
        return installDate != DATE_UNDEFINED;
    }
}
