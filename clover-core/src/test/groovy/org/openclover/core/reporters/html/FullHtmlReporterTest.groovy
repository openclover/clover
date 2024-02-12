package org.openclover.core.reporters.html

import clover.org.apache.velocity.VelocityContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openclover.core.CloverLicenseInfo
import org.openclover.core.CloverStartup
import org.openclover.runtime.Logger

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

class FullHtmlReporterTest {

    String ownerStmt

    @Before
    void setUp() throws Exception {
        CloverStartup.loadLicense(Logger.getInstance())
        ownerStmt = CloverLicenseInfo.OWNER_STMT + " "

    }

    @After
    void tearDown() {
        CloverLicenseInfo.EXPIRED = false
        CloverLicenseInfo.EXPIRES = false
    }

    @Test
    void testInsertLicenseWarnings() {
        VelocityContext ctx = new VelocityContext()
        CloverLicenseInfo.EXPIRED = false
        CloverLicenseInfo.EXPIRES = false

        HtmlReporter.insertLicenseMessages(ctx)
        assertEquals(ownerStmt, ctx.get("headerMsg"))
        assertEquals(ownerStmt, ctx.get("footerMsg"))
        assertNull(ctx.get("evalMsg"))

        ctx = new VelocityContext()
        CloverLicenseInfo.EXPIRED = false
        CloverLicenseInfo.EXPIRES = false
        HtmlReporter.insertLicenseMessages(ctx)
        assertEquals(ownerStmt + CloverLicenseInfo.PRE_EXPIRY_STMT, ctx.get("headerMsg"))
        assertEquals(ownerStmt + CloverLicenseInfo.PRE_EXPIRY_STMT, ctx.get("footerMsg"))
        assertNull(ctx.get("evalMsg"))

        ctx = new VelocityContext()
        CloverLicenseInfo.EXPIRED = false
        CloverLicenseInfo.EXPIRES = true

        HtmlReporter.insertLicenseMessages(ctx)
        assertNotNull(ctx.get("evalMsg"))
        assertEquals(ownerStmt + CloverLicenseInfo.PRE_EXPIRY_STMT, ctx.get("headerMsg"))
        assertEquals(ownerStmt + CloverLicenseInfo.PRE_EXPIRY_STMT, ctx.get("footerMsg"))

        ctx = new VelocityContext()
        CloverLicenseInfo.EXPIRED = true
        CloverLicenseInfo.EXPIRES = true
        HtmlReporter.insertLicenseMessages(ctx)
        assertNull(ctx.get("evalMsg"))
        assertEquals(ownerStmt + CloverLicenseInfo.POST_EXPIRY_STMT + " " + CloverLicenseInfo.CONTACT_INFO_STMT,
                ctx.get("headerMsg"))
        assertEquals(ownerStmt + CloverLicenseInfo.POST_EXPIRY_STMT, ctx.get("footerMsg"))
    }

}
