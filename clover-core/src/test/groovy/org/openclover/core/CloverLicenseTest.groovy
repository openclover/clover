package org.openclover.core

import org.junit.Test

import static org.junit.Assert.assertEquals

class CloverLicenseTest {

    @Test
    void testCommunityLicense() {
        CloverLicense lic = CloverLicenseDecoder.decode("<whatever input>")
        assertEquals("OpenClover", lic.getProductName())
        assertEquals("free and open-source", lic.getLicenseName())
        assertEquals("", lic.getOrganisation())
        assertEquals(0L, lic.getLicenseExpiryDate())
        assertEquals(0L, lic.getMaintenanceExpiryDate())
        assertEquals("", lic.getPreExpiryStatement())
        assertEquals("", lic.getPostExpiryStatement())
        assertEquals("OpenClover is free and open-source software.", lic.getOwnerStatement())
    }
}
