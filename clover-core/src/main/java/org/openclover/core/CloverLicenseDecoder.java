package org.openclover.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;

public class CloverLicenseDecoder {

    /**
     * The built-in license key
     */
    private static final CloverLicense DEFAULT_LICENSE = new CloverLicense("OpenClover", "free and open-source", "", 0L, 0L);

    public static CloverLicense decode(InputStream licenseIn) throws LicenseDecoderException {
        return decode(readFrom(licenseIn));
    }

    public static CloverLicense decode(String licenseKey) throws LicenseDecoderException {
        return DEFAULT_LICENSE;
    }

    public static CloverLicense createEvalLicense(long installTimestamp) {
        return DEFAULT_LICENSE;
    }

    private static String readFrom(InputStream licenseIn) throws LicenseDecoderException {
        try {
            final StringBuilder licenseCert = new StringBuilder();
            LineNumberReader lin = new LineNumberReader(new InputStreamReader(licenseIn, StandardCharsets.UTF_8));
            for (String line = lin.readLine(); line != null; line = lin.readLine()) {
                licenseCert.append(line).append("\n");
            }
            return licenseCert.toString();
        } catch (IOException e) {
            throw new LicenseDecoderException("Error reading license. " + e.getMessage(), e);
        }
    }
}
