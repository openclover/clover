package com.atlassian.clover.recorder

import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.CloverDatabaseSpec
import com.atlassian.clover.CodeType
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.ProgressListener
import org.openclover.runtime.RuntimeType
import org.openclover.runtime.api.CloverException
import com.atlassian.clover.api.registry.BlockMetrics
import com.atlassian.clover.api.registry.ClassInfo
import com.atlassian.clover.api.registry.SourceInfo
import com.atlassian.clover.cfg.Interval
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.context.ContextStore
import com.atlassian.clover.context.MethodRegexpContext
import com.atlassian.clover.context.StatementRegexpContext
import com.atlassian.clover.instr.InstrumentationSessionImpl
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.registry.entities.TestCaseInfo
import org.openclover.runtime.recorder.FileBasedPerTestRecording
import org.openclover.runtime.registry.format.RegAccessMode
import org.openclover.runtime.registry.format.RegHeader
import com.atlassian.clover.registry.metrics.ProjectMetrics
import org.openclover.buildutil.testutils.IOHelper
import org.openclover.runtime.util.CloverBitSet
import com.atlassian.clover.util.SimpleCoverageRange
import org_openclover_runtime.Clover
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import java.util.regex.Pattern

import static org.junit.Assert.*
import static org.openclover.util.Lists.newArrayList

class CloverDatabaseTest {

    private File tmpDir

    @Rule
    public TestName testName = new TestName()
    
    @Before
    void setUp() throws Exception {
        tmpDir = IOHelper.createTmpDir(testName.methodName)
    }

    @After
    void tearDown() throws Exception {
        if (!IOHelper.delete(tmpDir)) {
            throw new RuntimeException("Unable to delete temporary test directory ${tmpDir.absolutePath}")
        }
    }

    @Test
    void testSimpleRoundTrip() throws Exception {
        int coveragePc = 30
        CloverDatabase db = createSampleDatabase(coveragePc)
        assertEquals("PcCoveredElements", coveragePc, (int) (db.getModel(CodeType.APPLICATION).getMetrics().getPcCoveredElements() * 100))
    }

    @Test
    void testMergeIdentity() throws Exception {
        String initStr = createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 1, 1, 0, 0, 0 ] as int[]).getRegistryFile().getAbsolutePath()

        CloverDatabase db1 = new CloverDatabase(initStr)
        db1.loadCoverageData()

        CloverDatabaseSpec spec = new CloverDatabaseSpec(initStr)
        List<CloverDatabaseSpec> specs = newArrayList()
        specs.add(spec)
        performMergeAndCheck(specs, db1.getFullModel().getDataLength(), 0.4f, db1.getFullModel().getMetrics())
    }

    @Test
    void testUpdateIdentityMerge() throws Exception {
        String initStr = createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 1, 1, 0, 0, 0 ] as int[]).getRegistryFile().getAbsolutePath()

        CloverDatabase db1 = new CloverDatabase(initStr)
        db1.loadCoverageData()

        performMergeAndCheck(initStr, newArrayList(),
                db1.getFullModel().getDataLength(), 0.4f, db1.getFullModel().getMetrics())
    }

    @Test
    void testUpdateMerge() throws Exception {
        String initStr = createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 1, 1, 0, 0, 0 ] as int[]).getRegistryFile().getAbsolutePath()
        CloverDatabaseSpec dbspec2 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 0, 0, 0, 1, 1 ] as int[]))
        CloverDatabaseSpec dbspec3 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 0, 0, 1, 0, 0 ] as int[]))

        List<CloverDatabaseSpec> specs = newArrayList()
        specs.add(dbspec2)
        specs.add(dbspec3)

        CloverDatabase db1 = new CloverDatabase(initStr)
        db1.loadCoverageData()

        performMergeAndCheck(initStr, specs, 5, 1.0f, null)
    }

    @Test
    void testMergeTransitivity() throws Exception {

        CloverDatabaseSpec dbspec1 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 1, 1, 0, 0, 0 ] as int[]))
        CloverDatabaseSpec dbspec2 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 0, 0, 0, 1, 1 ] as int[]))
        CloverDatabaseSpec dbspec3 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [ 0, 0, 1, 0, 0 ] as int[]))

        List<CloverDatabaseSpec> specs = newArrayList()
        specs.add(dbspec1); specs.add(dbspec2); specs.add(dbspec3)
        CloverDatabase db123 = performMergeAndCheck(specs, 5, 1.0f, null)

        Collections.reverse(specs)
        CloverDatabase db321 = performMergeAndCheck(specs, 5, 1.0f, null)
        assertEquals(db123.getFullModel().getMetrics(), db321.getFullModel().getMetrics())

        specs.clear()
        specs.add(dbspec1); specs.add(dbspec2)
        CloverDatabase db12 = performMergeAndCheck(specs, 5, 0.8f, null)

        Collections.reverse(specs)
        CloverDatabase db21 = performMergeAndCheck(specs, 5, 0.8f, null)
        assertEquals(db12.getFullModel().getMetrics(), db21.getFullModel().getMetrics())

        specs.clear()
        specs.add(dbspec2); specs.add(dbspec3)
        CloverDatabase db23 = performMergeAndCheck(specs, 5, 0.6f, null)

        Collections.reverse(specs)
        CloverDatabase db32 = performMergeAndCheck(specs, 5, 0.6f, null)
        assertEquals(db23.getFullModel().getMetrics(), db32.getFullModel().getMetrics())

        specs.clear()
        specs.add(dbspec1); specs.add(dbspec3)
        CloverDatabase db13 = performMergeAndCheck(specs, 5, 0.6f, null)

        Collections.reverse(specs)
        CloverDatabase db31 = performMergeAndCheck(specs, 5, 0.6f, null)
        assertEquals(db31.getFullModel().getMetrics(), db13.getFullModel().getMetrics())


        specs.clear()
        specs.add(new CloverDatabaseSpec(db12.getInitstring()))
        specs.add(new CloverDatabaseSpec(db23.getInitstring()))
        CloverDatabase db1223 = performMergeAndCheck(specs, 5, 1.0f, db123.getFullModel().getMetrics())

        Collections.reverse(specs)
        performMergeAndCheck(specs, 5, 1.0f, db1223.getFullModel().getMetrics())

        specs.clear()
        specs.add(new CloverDatabaseSpec(db13.getInitstring()))
        specs.add(new CloverDatabaseSpec(db23.getInitstring()))
        CloverDatabase db1323 = performMergeAndCheck(specs, 5, 1.0f, db123.getFullModel().getMetrics())

        Collections.reverse(specs)
        performMergeAndCheck(specs, 5, 1.0f, db1323.getFullModel().getMetrics())

    }

    @Test
    void testContextStoreMerging() throws Exception {
        Clover2Registry db1 = createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [1, 1, 0, 0, 0] as int[])

        Clover2Registry db2 = createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [0, 0, 0, 1, 1] as int[])

        Clover2Registry db3 = createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [0, 0, 1, 0, 0] as int[])

        db1.getContextStore().addMethodContext(new MethodRegexpContext("m1", Pattern.compile(".*")))
        db2.getContextStore().addMethodContext(new MethodRegexpContext("m2", Pattern.compile(".*")))
        db3.getContextStore().addMethodContext(new MethodRegexpContext("m3", Pattern.compile(".*")))
        db3.getContextStore().addMethodContext(new MethodRegexpContext("m4", Pattern.compile("unique")))
        db1.getContextStore().addStatementContext(new StatementRegexpContext("s1", Pattern.compile(".*")))
        db2.getContextStore().addStatementContext(new StatementRegexpContext("s2", Pattern.compile(".*")))
        db3.getContextStore().addStatementContext(new StatementRegexpContext("s3", Pattern.compile(".*")))
        db3.getContextStore().addStatementContext(new StatementRegexpContext("s4", Pattern.compile("unique")))

        db1.saveAndOverwriteFile(); db2.saveAndOverwriteFile(); db3.saveAndOverwriteFile()

        List<CloverDatabaseSpec> specs = newArrayList()

        specs.add(new CloverDatabaseSpec(db1)); specs.add(new CloverDatabaseSpec(db2))
        specs.add(new CloverDatabaseSpec(db3))

        CloverDatabase db123 = performMergeAndCheck(specs, 5, 1.0f, null)

        ContextStore cs = db123.getContextStore()

        List contexts = cs.getAllUserContexts()
        assertEquals(3, cs.getMethodContexts().size())
        assertEquals(2, contexts.size())

    }

    @Test
    void testMergeFileVersions() throws Exception {
        int newest = 3000
        CloverDatabaseSpec dbspec1 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, newest,
                [1, 0, 0, 0, 0] as int[]))
        CloverDatabaseSpec dbspec2 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 4321, newest - 1,
                [0, 1, 0, 0, 0] as int[]))
        CloverDatabaseSpec dbspec3 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 4444, newest - 2,
                [0, 0, 1, 0, 0] as int[]))

        List<CloverDatabaseSpec> specs = newArrayList()
        // newest first
        specs.add(dbspec1); specs.add(dbspec2); specs.add(dbspec3)

        CloverDatabase db123 = performMergeAndCheck(specs, 5, 0.2f, null)

        ClassInfo cinfo = db123.getFullModel().findClass("a.b.c.d")
        assertEquals("timestamp", newest, cinfo.getContainingFile().getTimestamp())

        // oldest first
        Collections.reverse(specs)
        CloverDatabase db321 = performMergeAndCheck(specs, 5, 0.2f, db123.getFullModel().getMetrics())

        cinfo = db321.getFullModel().findClass("a.b.c.d")
        assertEquals("timestamp", newest, cinfo.getContainingFile().getTimestamp())
    }

    @Test
    void testMergeVersionCompacting() throws Exception {
        int newest = 3000
        CloverDatabaseSpec dbspec1 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, newest,
                [1, 0, 0, 0, 0] as int[]))
        CloverDatabaseSpec dbspec2 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 4321, newest - 1,
                [0, 1, 0, 0, 0] as int[]))
        CloverDatabaseSpec dbspec3 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 4444, newest - 2,
                [0, 0, 1, 0] as int[]))

        List<CloverDatabaseSpec> specs = newArrayList()
        // newest first - the first record encountered will be used, and the other two will be discarded
        specs.add(dbspec1); specs.add(dbspec2); specs.add(dbspec3)

        CloverDatabase db123 = performMergeAndCheck(specs, 5, 0.2f, null)

        ClassInfo cinfo = db123.getFullModel().findClass("a.b.c.d")
        assertEquals("timestamp", newest, cinfo.getContainingFile().getTimestamp())

        // oldest first - the first two records encountered will be discarded, creating two "holes" in the backing array
        // these holes should be removed by the compacting process
        Collections.reverse(specs)
        CloverDatabase db321 = performMergeAndCheck(specs, 5, 0.2f, db123.getFullModel().getMetrics())

        cinfo = db321.getFullModel().findClass("a.b.c.d")
        assertEquals("timestamp", newest, cinfo.getContainingFile().getTimestamp())
    }

    @Test
    void testMergeDisjointDatabases() throws Exception {
        CloverDatabaseSpec dbspec1 = new CloverDatabaseSpec(createSingleClassDatabase("a", "a", 32000, 1, 1,
                [1, 0, 0, 0, 0] as int[]))
        CloverDatabaseSpec dbspec2 = new CloverDatabaseSpec(createSingleClassDatabase("b", "b", 32000, 2, 2,
                [0, 1, 0, 0, 0] as int[]))
        CloverDatabaseSpec dbspec3 = new CloverDatabaseSpec(createSingleClassDatabase("c", "c", 32000, 3, 3,
                [0, 0, 1, 0, 0] as int[]))
        CloverDatabaseSpec dbspec4 = new CloverDatabaseSpec(createSingleClassDatabase("d", "d", 32000, 4, 4,
                [0, 0, 0, 1, 0] as int[]))

        List<CloverDatabaseSpec> specs = newArrayList()
        specs.add(dbspec1); specs.add(dbspec2); specs.add(dbspec3); specs.add(dbspec4)
        CloverDatabase merged = performMergeAndCheck(specs, 20, 0.2f, null)
        assertTrue(merged.getRegistry().isReadOnly())
        ProjectMetrics metrics = (ProjectMetrics) merged.getFullModel().getMetrics()
        assertEquals("packages", 4, metrics.getNumPackages())
        assertEquals("files", 4, metrics.getNumFiles())
        assertEquals("classes", 4, metrics.getNumClasses())
    }

    @Test
    void testMergeDatabaseMarking() throws Exception {
        CloverDatabaseSpec dbspec1 = new CloverDatabaseSpec(createSingleClassDatabase("a.b.c", "d", 32000, 1234, 4321,
                [1, 1, 0, 0, 0] as int[]))

        List<CloverDatabaseSpec> specs = Collections.singletonList(dbspec1)

        assertSame(RegHeader.readFrom(new File(dbspec1.getInitString())).getAccessMode(), RegAccessMode.READWRITE)

        final File mergedDb = File.createTempFile("merge", ".cdb", tmpDir)

        CloverDatabase.merge(specs, mergedDb.getAbsolutePath())

        assertSame(RegHeader.readFrom(mergedDb).getAccessMode(), RegAccessMode.READONLY)
    }

    @Test
    void testCopyForBackgroundCoverageLoad() throws IOException, CloverException {
        CloverDatabase db1 = createSampleDatabase(50)

        final Clover2Registry registry = db1.getRegistry()
        int len = registry.getDataLength()
        prepareCoverageDataFile(registry, (len / 4) as int, (3 * len / 4) as int); // previous 50% + new 50% shifted by 25% = 75%

        CloverDatabase db2 = db1.copyForBackgroundCoverageDataLoad()
        db2.loadCoverageData()

        assertEquals("Original database should remain at 50% coverage, coverage", 0.50, db1.getModel(CodeType.APPLICATION).getMetrics().getPcCoveredElements(), 0.01)
        assertEquals("New database should be at 75% coverage, coverage", 0.75, db2.getModel(CodeType.APPLICATION).getMetrics().getPcCoveredElements(), 0.01)
    }

    @Test
    void testSubsequentCoverageLoadsConstantSpan() throws IOException, CloverException {
        CloverDatabase db1 = createSampleDatabase(50)
        int len1 = db1.getRegistry().getDataLength()
        assertEquals("First load should have a TCI", 1, db1.getTestHits(new SimpleCoverageRange(0, len1)).size())

        CloverDatabase db2 = db1.copyForBackgroundCoverageDataLoad()
        db2.loadCoverageData(generateCoverageDataSpec()); // just simulate coverage load

        int len2 = instrumentSampleFile(db2.getRegistry(), 1)
        assertEquals("Changed file should have no touching tests", 0, db2.getTestHits(new SimpleCoverageRange(len1, len2)).size())
        prepareCoverageDataFile(db2.getRegistry(), len1, len2)

        CloverDatabase db3 = db2.copyForBackgroundCoverageDataLoad()
        db3.loadCoverageData(generateCoverageDataSpec())
        final Set<TestCaseInfo> testCaseInfos = db3.getTestHits(new SimpleCoverageRange(len1, len2))
        assertEquals("Second load should have a TCI", 1, testCaseInfos.size())

    }

    @Test
    void testSubsequentCoverageLoadsCalculatedSpan() throws IOException, CloverException {
        // this one simulates what CIJ does - may seem artificail but that is what happens in runtime
        CloverDatabase db1 = createSampleDatabase(50)
        final long firstTimeStamp = db1.getRecordingTimestamp()
        final int len1 = db1.getRegistry().getDataLength()
        assertEquals("First load should have a TCI", 1, db1.getTestHits(new SimpleCoverageRange(0, len1)).size())

        CloverDatabase db2 = db1.copyForBackgroundCoverageDataLoad()
        db2.loadCoverageData(generateCoverageDataSpec(prepareCIJSpan(firstTimeStamp, db2)))

        int len2 = instrumentSampleFile(db2.getRegistry(), 1)
        assertEquals("Changed file should have no touching tests", 0, db2.getTestHits(new SimpleCoverageRange(len1, len2)).size())
        prepareCoverageDataFile(db2.getRegistry(), len1, len2)

        CloverDatabase db3 = db2.copyForBackgroundCoverageDataLoad()
        db3.loadCoverageData(generateCoverageDataSpec(prepareCIJSpan(firstTimeStamp, db3)))
        final Set<TestCaseInfo> testCaseInfos = db3.getTestHits(new SimpleCoverageRange(len1, len2))
        assertEquals("Second load should have a TCI", 1, testCaseInfos.size())

    }

    private long prepareCIJSpan(long startTs, CloverDatabase db) {
        return db.getRegistry().getVersion() - startTs - 1
    }


    private CloverDatabase performMergeAndCheck(List<CloverDatabaseSpec> specs, int expectedDataLength, float expectedPcCovered, BlockMetrics expectedMetrics) throws Exception {
        return performMergeAndCheck(null, specs, expectedDataLength, expectedPcCovered, expectedMetrics)
    }

    private CloverDatabase performMergeAndCheck(String updateInitString, List<CloverDatabaseSpec> specs, int expectedDataLength, float expectedPcCovered, BlockMetrics expectedMetrics) throws Exception {
        String dbname = updateInitString != null ? updateInitString : File.createTempFile("merge", ".cdb", tmpDir).getAbsolutePath()
        CloverDatabase.merge(specs, dbname, updateInitString != null, Interval.DEFAULT_SPAN, ProgressListener.NOOP_LISTENER)
        CloverDatabase db = new CloverDatabase(dbname)
        db.loadCoverageData()
        assertEquals("data length", expectedDataLength, db.getFullModel().getDataLength())
        assertEquals(expectedPcCovered, db.getFullModel().getMetrics().getPcCoveredElements(), 0.01f)
        if (expectedMetrics != null) {
            assertEquals(expectedMetrics, db.getFullModel().getMetrics())
        }
        return db
    }

    private Clover2Registry createSingleClassDatabase(String pkg, String cn, long filesize, long checksum, long timestamp, int[] coverage) throws Exception {

        File registryFile = File.createTempFile("registry", ".cdb", tmpDir)

        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)
        SourceInfo region = new FixedSourceRegion(1, 2, 3, 4)
        ContextSet context = new ContextSet()
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()

        session.enterFile(pkg, new File(pkg.replace('.', '/') + (pkg.length() > 0 ? "/" : "") + cn + ".java"), 0, 0, timestamp, filesize, checksum)
        session.enterClass(cn, region, new Modifiers(), false, false, false)
        session.enterMethod(context, region, new MethodSignature("method0"), false)
        // one slot left here for the method entry
        for (int i = 0; i < coverage.length - 1; i++) {
            session.addStatement(context, region, 0)
        }
        session.exitMethod(0, 0)
        session.exitClass(10, 0)
        session.exitFile()
        final long whenCoverage = session.finishAndApply().getVersion() + 1
        registry.saveAndOverwriteFile()

        String recname = Clover.getRecordingName(hashCode(), registry.getRegistryFile().getAbsolutePath(), whenCoverage)
        FileBasedGlobalCoverageRecording.flushToDisk(recname, registry.getVersion(), System.currentTimeMillis(), coverage)

        return registry
    }

    private CoverageDataSpec generateCoverageDataSpec() {
        return generateCoverageDataSpec(Long.MAX_VALUE)
    }

    private CoverageDataSpec generateCoverageDataSpec(long span) {
        return new CoverageDataSpec(null, span, false, true, true, true, PerTestCoverageStrategy.SAMPLING)
    }

    private CloverDatabase createSampleDatabase(int coveragePc) throws IOException, CloverException {
        File registryFile = File.createTempFile("registry", ".cdb", tmpDir)

        // build a simple registry
        Clover2Registry registry = new Clover2Registry(registryFile, testName.methodName)
        int len = instrumentSampleFile(registry, 0)
        assertTrue("data length must be a multiple of 100", len > 0 && len % 100 == 0)

        registry.saveAndOverwriteFile()
        // write a suitable recording file
        prepareCoverageDataFile(registry, 0, (coveragePc * len / 100) as int)

        // read the whole lot back in
        CloverDatabase db = new CloverDatabase(registry.getRegistryFile().getAbsolutePath())
        db.loadCoverageData(generateCoverageDataSpec())
        return db
    }

    private int instrumentSampleFile(Clover2Registry registry, int fileVersion) throws CloverException {
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        SourceInfo region = new FixedSourceRegion(1, 2, 3, 4)
        ContextSet context = new ContextSet()

        session.enterFile("", new File("Foo.java"), 10, 20, 30 + fileVersion, 40 + fileVersion, 50 + fileVersion)
        session.enterClass("Foo", region, new Modifiers(), false, false, false)

        for (int i = 0; i < 40; i++) {
            context = context.set(ContextStore.CONTEXT_METHOD)
            session.enterMethod(context, region, new MethodSignature("method$i"), false)
            context = context.set(ContextStore.CONTEXT_IF)
            session.addStatement(context, region, 0)
            session.addStatement(context, region, 0)
            session.addStatement(context, region, 0)
            context = context.set(ContextStore.CONTEXT_WHILE)
            session.addStatement(context, region, 0)
            session.exitMethod(0, 0)
        }
        session.exitClass(10, 0)
        session.exitFile()
        session.finishAndApply()

        return registry.getProject().getDataLength()
    }

    private int runId = 0
    private int sliceId = 0

    private void prepareCoverageDataFile(Clover2Registry registry, int from, int to) throws IOException {
        int length = registry.getProject().getDataLength()
        int[] data = new int[length]; // this is already initialized to 0's

        Arrays.fill(data, from, to, 1)

        CloverBitSet testCoverage = new CloverBitSet(length)
        for (int i = from; i < to; i++) {
            testCoverage.add(i)
        }

        String recname = Clover.getRecordingName(hashCode(), registry.getRegistryFile().getAbsolutePath(), System.currentTimeMillis())
        FileBasedGlobalCoverageRecording.flushToDisk(recname, registry.getVersion(), System.currentTimeMillis(), data)
        new FileBasedPerTestRecording(registry.getRegistryFile().getAbsolutePath(), registry.getVersion(), 0,
                testCoverage, "testMethod", "testMethodAtRuntime",
                getUniqueTimestamp(), getUniqueTimestamp(), 0.001,
                new RuntimeType("TestClass"), sliceId++, runId++, 0, null).transcribe()

    }

    private long lastTimestamp

    private long getUniqueTimestamp() {
        long now = System.currentTimeMillis()
        if (now > lastTimestamp) {
            lastTimestamp = now
        } else {
            lastTimestamp++
        }
        return lastTimestamp
    }
}
