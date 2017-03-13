package com.atlassian.clover

import org.junit.Test

import static org.junit.Assert.assertEquals

class CloverLicenseTest {

    @Test
    void testCommunityLicense() {
        CloverLicense lic = CloverLicenseDecoder.decode("<whatever input>")
        assertEquals("Clover", lic.getProductName())
        assertEquals("free", lic.getLicenseName())
        assertEquals("", lic.getOrganisation())
        assertEquals(0L, lic.getLicenseExpiryDate())
        assertEquals(0L, lic.getMaintenanceExpiryDate())
        assertEquals("", lic.getPreExpiryStatement())
        assertEquals("", lic.getPostExpiryStatement())
        assertEquals("Clover free edition.", lic.getOwnerStatement())
    }
}
