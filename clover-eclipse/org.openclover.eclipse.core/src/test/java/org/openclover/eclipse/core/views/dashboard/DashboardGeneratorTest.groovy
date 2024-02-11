package org.openclover.eclipse.core.views.dashboard

import com.atlassian.clover.CloverDatabase
import org.openclover.runtime.api.registry.CloverRegistryException
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.util.FileUtils
import org.hamcrest.CoreMatchers
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

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
            final String dasboardContent = readFile(dashboardFile)
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

    private static String readFile(File inputFile) {
        char[] buffer = new char[16384]
        StringBuilder out = new StringBuilder()
        Reader fileReader = null

        try {
            fileReader = new BufferedReader(new FileReader(inputFile))
            int size
            while ( (size = fileReader.read(buffer)) != -1 ) {
                out.append(buffer, 0, size)
            }
        } catch (IOException ex) {
            fail(ex.toString())
        } finally {
            fileReader?.close()
        }

        out.toString()
    }
}
