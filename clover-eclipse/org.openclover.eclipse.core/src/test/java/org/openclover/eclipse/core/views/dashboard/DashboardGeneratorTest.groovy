package org.openclover.eclipse.core.views.dashboard

import clover.com.google.common.io.Files
import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.api.registry.CloverRegistryException
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.util.FileUtils
import org.hamcrest.CoreMatchers
import org.junit.Test

import java.nio.charset.Charset

import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class DashboardGeneratorTest {

    private static Clover2Registry createSampleRegistryAt(File dbLocation) throws IOException, CloverRegistryException {
        Clover2Registry reg = new Clover2Registry(dbLocation, "Test")
        reg.saveAndOverwriteFile()
        return reg
    }

    @Test
    void testExecute() throws Exception {
        File cloverDb = null
        File reportDir = null

        try {
            // create sample database
            cloverDb = File.createTempFile("clover", ".db")
            cloverDb.deleteOnExit()
            createSampleRegistryAt(cloverDb)

            reportDir = FileUtils.createTempDir("eclipse-dashboard")
            reportDir.deleteOnExit()

            // generate dashboard for eclipse
            DashboardGenerator generator = new DashboardGenerator(
                    new CloverDatabase(cloverDb.getAbsolutePath()), reportDir)
            generator.execute()

            // check content
            final File dashboardFile = new File(reportDir, "dashboard-eclipse.html")
            assertTrue(dashboardFile.exists())
            final String dasboardContent = Files.toString(dashboardFile, Charset.forName("UTF-8"))
            assertThat(dasboardContent, CoreMatchers.containsString("Coverage"))
            assertThat(dasboardContent, CoreMatchers.containsString("Test results"))
            assertThat(dasboardContent, CoreMatchers.containsString("Most complex packages"))
            assertThat(dasboardContent, CoreMatchers.containsString("Most complex classes"))
            assertThat(dasboardContent, CoreMatchers.containsString("Top 0 project risks"))
            assertThat(dasboardContent, CoreMatchers.containsString("Least tested methods"))

        } finally {
            if (cloverDb != null) {
                cloverDb.delete()
            }
            if (reportDir != null) {
                FileUtils.deltree(reportDir)
            }
        }
    }
}
