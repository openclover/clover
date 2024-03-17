package org.openclover.core.reporters

import junit.framework.TestCase
import org.openclover.core.TestUtils
import org.openclover.core.registry.Clover2Registry
import org.openclover.runtime.CloverNames
import org.openclover.runtime.api.CloverException
import org_openclover_runtime.Clover

class CloverReporterTest extends TestCase {
    void setUp() {
        Clover.resetRecorders()
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
