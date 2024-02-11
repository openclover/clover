package com.atlassian.clover.reporters

import com.atlassian.clover.CloverLicenseDecoder
import org.openclover.runtime.api.CloverException
import com.atlassian.clover.CloverLicense
import org.openclover.runtime.CloverNames
import com.atlassian.clover.CloverStartup
import org.openclover.runtime.Logger
import com.atlassian.clover.TestUtils
import com.atlassian.clover.registry.Clover2Registry
import junit.framework.TestCase

import com_atlassian_clover.Clover

class CloverReporterTest extends TestCase {
    void setUp() {
        Clover.resetRecorders()
        setUpLicense()
    }

    void testReportCompletesIfOutOfVmCoverageRecording() throws Exception {
        final File dbFile = newDbFile(TestUtils.createEmptyDirFor(getClass(), getName()))

        createNewRegistry(dbFile)

        newLiveRecFile(dbFile)

        Current cfg = newCurrentCfg(dbFile)

        long start = System.currentTimeMillis()
        CloverReporter.buildReporter(cfg).execute()
        long end = System.currentTimeMillis()

        assertFalse(new File(dbFile.getAbsolutePath() + CloverNames.LIVEREC_SUFFIX).exists())
        assertTrue(end - start >= 5000)
    }

    void testReportCompletesIfInVmCoverageRecording() throws Exception {
        final File dbFile = newDbFile(TestUtils.createEmptyDirFor(getClass(), getName()))

        createNewRegistry(dbFile)

        newLiveRecFile(dbFile)

        Clover.getRecorder(dbFile.getAbsolutePath(), 0, 0, 0, null, null)

        Current cfg = newCurrentCfg(dbFile)

        long start = System.currentTimeMillis()
        CloverReporter.buildReporter(cfg).execute()
        long end = System.currentTimeMillis()

        assertFalse(new File(dbFile.getAbsolutePath() + CloverNames.LIVEREC_SUFFIX).exists())
    }

    private void newLiveRecFile(File dbFile) throws IOException {
        final File liveRecFile = new File(dbFile.getAbsolutePath() + CloverNames.LIVEREC_SUFFIX)
        liveRecFile.createNewFile()
    }

    private Current newCurrentCfg(File dbFile) throws IOException {
        Current cfg = new Current(Current.DEFAULT_HTML)
        cfg.setInitString(dbFile.getAbsolutePath())
        cfg.setReportDelay(5000)

        final File outFile = File.createTempFile("clover", "report")
        outFile.delete()
        cfg.setOutFile(outFile)
        return cfg
    }

    private void setUpLicense() {
        CloverStartup.setLicenseLoader(new CloverStartup.LicenseLoader() {
            CloverLicense loadLicense(Logger log) {
                try {
                    return CloverLicenseDecoder.decode("")
                } catch (Exception e) {
                    return null
                }
            }
        })
        CloverStartup.loadLicense(Logger.getInstance(), true)
    }

    private void createNewRegistry(File dbFile) throws IOException, CloverException {
        Clover2Registry reg = Clover2Registry.createOrLoad(dbFile, "My Database")
        reg.saveAndOverwriteFile()
    }

    private File newDbFile(File parentDir) throws IOException {
        final File dbFile = File.createTempFile("clover", ".db", parentDir)
        dbFile.deleteOnExit()
        dbFile.delete()
        return dbFile
    }
}
