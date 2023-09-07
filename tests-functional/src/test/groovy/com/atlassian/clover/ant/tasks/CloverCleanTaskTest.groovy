package com.atlassian.clover.ant.tasks

import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.api.CloverException
import com.atlassian.clover.optimization.Snapshot

class CloverCleanTaskTest extends CloverBuildFileTestBase {

    CloverCleanTaskTest(String aTestName) {
        super(aTestName)
    }

    String getAntFileName() {
        return "clover-clean.xml"
    }

    void testKeepDBTrue() {
        assertTrue(util.getCoverageDb().exists())
        assertTrue(getCoverageRecordings().length > 0)
        executeTarget("testKeepDBTrue")
        assertTrue(getCoverageRecordings().length == 0)
        assertTrue(util.getCoverageDb().exists())
    }

    void testKeepDBFalse() {
        assertTrue(util.getCoverageDb().exists())
        assertTrue(getCoverageRecordings().length > 0)
        executeTarget("testKeepDBFalse")
        assertTrue(getCoverageRecordings().length == 0)
        assertTrue(!util.getCoverageDb().exists())
    }

    void testKeepSnapshotTrue() throws IOException, CloverException {
        assertTrue(util.getCoverageDb().exists())
        assertTrue(getCoverageRecordings().length > 0)
        createSnapshot()
        executeTarget("testKeepSnapshotTrue")
        assertTrue(getCoverageRecordings().length == 0)
        assertTrue(!util.getCoverageDb().exists())
        assertTrue(new File(Snapshot.fileNameForInitString(util.getCoverageDb().getAbsolutePath())).exists())
    }

    void testKeepSnapshotFalse() throws IOException, CloverException {
        assertTrue(util.getCoverageDb().exists())
        assertTrue(getCoverageRecordings().length > 0)
        createSnapshot()
        executeTarget("testKeepSnapshotFalse")
        assertTrue(getCoverageRecordings().length == 0)
        assertTrue(!util.getCoverageDb().exists())
        assertTrue(!new File(Snapshot.fileNameForInitString(util.getCoverageDb().getAbsolutePath())).exists())
    }

    private void createSnapshot() throws IOException, CloverException {
        Snapshot.generateFor(CloverDatabase.loadWithCoverage(util.getCoverageDb().getAbsolutePath(), new CoverageDataSpec())).store()
    }

    void testNoSetup() {
        executeTarget("testNoSetup")
    }

    private File[] getCoverageRecordings() {
        final String coverageDbName = util.getCoverageDb().getName()
        File[] ls = util.getCoverageDb().getParentFile().listFiles(new FilenameFilter() {
            boolean accept(File dir, String name) {
                return name.startsWith(coverageDbName) && 
                        name.compareTo(coverageDbName) != 0 && 
                        name.compareTo(Snapshot.fileNameForInitString(coverageDbName)) != 0
            }
        })
        return ls
    }
}
