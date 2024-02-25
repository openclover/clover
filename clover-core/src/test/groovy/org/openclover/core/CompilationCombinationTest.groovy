package org.openclover.core

import junit.framework.TestCase
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.ContextSet
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.api.registry.SourceInfo
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.instr.InstrumentationSessionImpl
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.FixedSourceRegion
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.registry.metrics.ClassMetrics
import org.openclover.core.registry.metrics.PackageMetrics
import org.openclover.core.util.FileUtils
import org.openclover.runtime.CloverProperties
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder
import org_openclover_runtime.Clover

/**
 * Tests combinations of instrumentation, compilation, multi-compilation,
 * and multi-execution
 */
class CompilationCombinationTest extends TestCase {
    private static final String PKGNAME = "com.test"
    private static final String CLASSNAME = "TestClass"

    private static class LocationIds {
        int classID
        int methID
        int stmtID
        long registryID
        File file
    }

    public String mInitString

    private File workingDir

    protected void setUp() throws Exception {
        workingDir = File.createTempFile(this.getClass().getName(),".tmp")
        workingDir.delete()
        workingDir.mkdir()
        setupDB()
    }

    private void setupDB() throws IOException {

        File coverageDbFile = File.createTempFile("clover_test_db",".tmp", workingDir)
        coverageDbFile.delete()
        mInitString = coverageDbFile.getAbsolutePath()
    }

    protected void tearDown() {
       FileUtils.deltree(workingDir)
    }

    /**
     * just one instrumentation, and a couple of runs
     * @throws Exception
     */
    void testSimpleCase() throws Exception {

        File tmpFile = File.createTempFile("test_","java", workingDir)

        LocationIds ids = instrumentFile(tmpFile)

        final LocationIds[] allIds = [ ids ]

        // "run" the instrumentation
        recordExecution(allIds)
        // browser the instrumentation
        assertRecordedRunsCorrect(allIds, [ 1 ] as int[])

        // run it again
        recordExecution(allIds)
        // and check it again
        assertRecordedRunsCorrect(allIds, [ 2 ] as int[])

        tmpFile.delete()
    }


    /**
     * This tests the case where we have two seperate "javac"
     * compliations over two disjoint file sets.
     * <p>
     * This high-lights a bug Cortrack-6457 (as at 6March2003)
     * that is causing jboss instrumentation to return 0% coverage.
     * <p>
     * <b>THIS BUG IS INTERMITANT</b>
     * <p>
     * This causes two "database revisions".
     * <p>
     * When we execute the instrumented code, we expect it
     * to be able to record the coverage from both "revisions".
     * <p>
     * When we browse the run-records, we expect to be
     * able to see the recordings against the "earlier" db revision
     * (that is; from the first 'javac")
     *
     */
    void testTwoDisjointCompilations() throws Exception {

        // two files
        File tmpFile1 = File.createTempFile("test1_","java", workingDir)
        File tmpFile2 = File.createTempFile("test2_","java", workingDir)

        // compile one file
        LocationIds ids1 = instrumentFile(tmpFile1)
        // compile the second file
        LocationIds ids2 = instrumentFile(tmpFile2)

        // "run" and hit file 1
        recordExecution([ ids1 ] as LocationIds[])
        // "run" and hit file 2
        recordExecution([ ids2 ] as LocationIds[])

        int counts1 = 1
        int counts2 = 1
        // browser the instrumentation
        assertRecordedRunsCorrect([ ids1, ids2 ] as LocationIds[],
                                  [ counts1, counts2 ] as int[])

        // now do more hits and re-compilations
        for (int i = 0; i < 10; i++) {
            ids2 = instrumentFile(tmpFile2)

            recordExecution([ ids1 ] as LocationIds[])
            counts1++
            recordExecution([ ids2 ] as LocationIds[])
            counts2++
            recordExecution([ ids2 ] as LocationIds[])
            counts2++

            assertRecordedRunsCorrect([ ids1, ids2 ] as LocationIds[],
                                      [ counts1, counts2 ] as int[])
        }


    }

    private void assertRecordedRunsCorrect(LocationIds[] aFiles, int[] aExpectedHitsCounts) throws Exception {
        long span = 1000L * 60L * 60
        CloverDatabase db = new CloverDatabase(mInitString)
        db.loadCoverageData(new CoverageDataSpec(span))

        ProjectInfo model = db.getModel(CodeType.APPLICATION)
        assertNotNull(model)

        PackageInfo pkg = model.getNamedPackage(PKGNAME)

        assertNotNull(pkg)
        assertEquals(aFiles.length, ((PackageMetrics)pkg.getMetrics()).getNumFiles())

        List files = pkg.getFiles()

        for (Iterator it = files.iterator(); it.hasNext();) {
            FullFileInfo fi = (FullFileInfo) it.next()

            // find the entry in aFiles that matches fi:
            LocationIds file = null
            int expectedHitsCount = -1
            for (int i = 0; i < aFiles.length; i++) {
                LocationIds f = aFiles[i]
                if (f.file.equals(fi.getPhysicalFile())) {
                    file = f
                    expectedHitsCount = aExpectedHitsCounts[i]
                }
            }
            assertNotNull("could not find file " + fi.getPhysicalFile() + " in aFiles", file)

            assertNotNull(fi)
            assertEquals(file.file, fi.getPhysicalFile())

            String fname = file.file.getName()

            final ClassInfo ci = fi.getNamedClass(CLASSNAME)
            assertNotNull(fname, ci)
            final ClassMetrics m = (ClassMetrics)ci.getMetrics()
            assertEquals(fname, 1, m.getNumMethods())
            assertEquals(fname, 1, m.getNumStatements())

            // both the method and statement should be hit the same
            assertEquals(fname, expectedHitsCount, ci.getMethods().get(0).getHitCount())
            assertEquals(fname, expectedHitsCount, ci.getMethods().get(0).getStatements().get(0).getHitCount())
        }
    }

    private void recordExecution(LocationIds[] aIds) {
        for (LocationIds id : aIds) {
            FixedSizeCoverageRecorder recorder = (FixedSizeCoverageRecorder) Clover.createRecorder(mInitString,
                    id.registryID, 0L, id.stmtID + 1, null, CloverProperties.newEmptyProperties())
            recorder.startRun()

            Thread shutdownFlusher = recorder.getShutdownFlusher()
            Runtime.getRuntime().removeShutdownHook(shutdownFlusher)

            recorder.inc(id.methID)
            recorder.inc(id.stmtID)
            recorder.forceFlush()
        }
    }

    private LocationIds instrumentFile(File aFile) throws Exception {
        // instrument a file
        Clover2Registry registry = Clover2Registry.fromInitString(mInitString, getName())
        LocationIds ids = new LocationIds()
        ids.file = aFile
        ContextSet con = new ContextSetImpl()
        SourceInfo reg = new FixedSourceRegion(0, 0)
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        session.enterFile(PKGNAME, aFile, 1, 1,  0L, 0L, 0L)
        // one class
        ids.classID = session.enterClass(CLASSNAME, reg, new Modifiers(), false, false, false).getDataIndex()
        // one method
        ids.methID = session.enterMethod(con, reg, new MethodSignature("foo"), false).getDataIndex()
        // one statement
        ids.stmtID = session.addStatement(con, reg, 1).getDataIndex()
        session.exitMethod(0, 0)
        session.exitClass(0, 0)
        session.exitFile()
        session.finishAndApply()
        registry.saveAndOverwriteFile()
        ids.registryID = registry.getVersion()
        return ids
    }
}
