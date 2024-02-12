package org.openclover.core.registry.format

import org.openclover.core.registry.ModelBuilder
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.FullProjectUpdate
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.TestCaseInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.context.ContextStore
import org.openclover.core.CoverageData
import org.openclover.core.recorder.PerTestCoverage
import org.openclover.core.recorder.InMemPerTestCoverage
import org.openclover.core.recorder.PerTestRecordingTranscript
import org.openclover.runtime.RuntimeType
import org.openclover.runtime.registry.format.RegAccessMode
import org.openclover.runtime.util.CloverBitSet
import org.openclover.runtime.api.registry.CloverRegistryException
import org.junit.After
import org.junit.Test

import static org.junit.Assert.*

class RegFileFormatTest {
    private static int TCI_ID = 0
    private static int SLICE_ID = 0
    private static int TEST_RUN_ID = 0

    @After
    void tearDown() throws Exception {
        TestCaseInfo.Factory.reset()
    }

    @Test
    void testSingleRoundTrip() throws Exception {
        final File temp = File.createTempFile("regfile", "test")
        temp.deleteOnExit()
        temp.delete()

        final ModelBuilder modelBuilder = newTestModel()

        long now = System.currentTimeMillis()
        final FullProjectInfo proj = (FullProjectInfo)modelBuilder.get("proj")
        final ContextStore ctxStore = new ContextStore()
        final RegFile regFile =
            new FreshRegFile(temp, RegAccessMode.READWRITE, "Test Registry")
                .save(new FullProjectUpdate(proj, ctxStore, now, now + 1))

        final UpdatableRegFile loadedRegFile = new UpdatableRegFile(temp)
        assertEquals("Test Registry", loadedRegFile.getName())
        assertSame(RegAccessMode.READWRITE, loadedRegFile.getHeader().getAccessMode())
        assertEquals(proj.getVersion(), loadedRegFile.getHeader().getVersion())
        
        loadedRegFile.readContents(new RegContentsConsumer() {
            void consume(RegContents contents) throws IOException, CloverRegistryException {
                final Iterator<InstrSessionSegment> sessions = contents.getSessions().iterator()
                final InstrSessionSegment session = sessions.next()
                //Only one
                assertFalse(sessions.hasNext())
                //It has the right stuff
                assertEquals(proj.getVersion(), session.getVersion())
                assertEquals(ctxStore, session.getCtxStore())
                assertEquals(3, session.getFileInfoRecords().size())
            }
        })
    }

    @Test
    void testIncrementalRoundTrip() throws Exception {
        final File temp = File.createTempFile("regfile", "test")
        temp.deleteOnExit()
        temp.delete()

        final ModelBuilder modelBuilder = newTestModel()
        final ContextStore ctxStore = new ContextStore()

        long now = System.currentTimeMillis()
        final RegFile regFile = new FreshRegFile(temp, RegAccessMode.READWRITE, "Test Registry").save(
                new IncrementalRegUpdate(
                        0l, now, now + 1, 1000,
                        [ (FullFileInfo)modelBuilder.get("Baz.java") ], ctxStore)).save(
                new IncrementalRegUpdate(
                        1l, now + 2, now + 3, 2000,
                        [ (FullFileInfo)modelBuilder.get("Bar.java") ], ctxStore)).save(
                new IncrementalRegUpdate(
                        2l, now + 4, now + 5, 3000,
                        [ (FullFileInfo)modelBuilder.get("Bing.java"), (FullFileInfo)modelBuilder.get("Baz.java") ], ctxStore))

        final UpdatableRegFile loadedRegFile = new UpdatableRegFile(temp)
        assertEquals("Test Registry", loadedRegFile.getName())
        assertSame(RegAccessMode.READWRITE, loadedRegFile.getHeader().getAccessMode())
        assertEquals(3000, loadedRegFile.getSlotCount())
        assertEquals(2l, loadedRegFile.getHeader().getVersion())
        
        loadedRegFile.readContents(new RegContentsConsumer() {
            void consume(RegContents contents) throws IOException, CloverRegistryException {
                final Iterator<InstrSessionSegment> sessions = contents.getSessions().iterator()
                final InstrSessionSegment[] threeSessions =
                        [ sessions.next(), sessions.next(), sessions.next() ] as InstrSessionSegment[]
                assertEquals(2l, threeSessions[0].getVersion())
                assertEquals(ctxStore, threeSessions[0].getCtxStore())
                assertEquals(2, threeSessions[0].getFileInfoRecords().size())
                assertEquals(1l, threeSessions[1].getVersion())
                assertEquals(ctxStore, threeSessions[1].getCtxStore())
                assertEquals(1, threeSessions[1].getFileInfoRecords().size())
                assertEquals(0l, threeSessions[2].getVersion())
                assertEquals(ctxStore, threeSessions[2].getCtxStore())
                assertEquals(1, threeSessions[2].getFileInfoRecords().size())
            }
        })
    }

    @Test
    void testWithCoverage() throws Exception {
        final File temp = File.createTempFile("regfile", "test")
        temp.deleteOnExit()
        temp.delete()

        final ModelBuilder modelBuilder = newTestModel()
        final ContextStore ctxStore = new ContextStore()
        final FullProjectInfo proj = (FullProjectInfo)modelBuilder.get("proj")

        long now = System.currentTimeMillis()
        final int[] testHits = [ 0, 1, 0, 1 ]
        final RegFile regFile = new FreshRegFile(
                temp,
                RegAccessMode.READONLY,
                "Test Registry",
                new CoverageData(
                    proj.getVersion() + 1, testHits,
                    buildPerTestData(
                        testHits, [ ["Baz.java", "Baz", "Baz.testMethod"] ] as String[][], modelBuilder)))
                .save(new FullProjectUpdate(proj, ctxStore, now, now + 1))

        final UpdatableRegFile loadedRegFile = new UpdatableRegFile(temp)
        assertEquals("Test Registry", loadedRegFile.getName())
        assertSame(RegAccessMode.READONLY, loadedRegFile.getHeader().getAccessMode())
        assertEquals(proj.getVersion(), loadedRegFile.getHeader().getVersion())

        loadedRegFile.readContents(new RegContentsConsumer() {
            void consume(RegContents contents) throws IOException, CloverRegistryException {
                assertNotNull(contents.getCoverage())
                assertTrue(Arrays.equals(testHits, contents.getCoverage().getHitCounts()))

                final Iterator<InstrSessionSegment> sessions = contents.getSessions().iterator()
                final InstrSessionSegment session = sessions.next()
                //Only one
                assertFalse(sessions.hasNext())
                //It has the right stuff
                assertEquals(proj.getVersion(), session.getVersion())
                assertEquals(ctxStore, session.getCtxStore())
                assertEquals(3, session.getFileInfoRecords().size())
            }
        })
    }

    private PerTestCoverage buildPerTestData(
        int[] coverage,
        String[][] fileClassMethods,
        ModelBuilder modelBuilder) {
        final InMemPerTestCoverage perTestCoverage = new InMemPerTestCoverage(coverage.length)

        for (String[] fileClassMethod : fileClassMethods) {
            final long now = System.currentTimeMillis()
            final long then = now + 1
            perTestCoverage.addCoverage(
                new TestCaseInfo(
                        Integer.valueOf(incTCIId()),
                        (FullClassInfo)modelBuilder.get(fileClassMethod[1]),
                        (FullMethodInfo)modelBuilder.get(fileClassMethod[2]),
                        null),
                new PerTestRecordingTranscript(
                        CloverBitSet.forHits(coverage),
                        new File(fileClassMethod[0]),
                        ((FullProjectInfo)modelBuilder.get("proj")).getVersion(),
                        fileClassMethod[2],
                        null,
                        now, then, 0.001, new RuntimeType(fileClassMethod[1]),
                        incSliceId(), incTestRunId(), 0, null)
            )
        }
        return perTestCoverage
    }

    private static int incTestRunId() {
        return TEST_RUN_ID++
    }

    private static int incSliceId() {
        return SLICE_ID++
    }

    private static int incTCIId() {
        return TCI_ID++
    }

    private static ModelBuilder newTestModel() {
        final ModelBuilder modelBuilder = new ModelBuilder()
        modelBuilder
            .proj("My Project").withId("proj")
                .pkg("com.foo.bar")
                    .file("Baz.java").withId("Baz.java")
                        .clazz("Baz.java")
                            .testMethod("testMethod").withId("Baz.testMethod").endInClass()
                        .endInFile()
                    .end()
                    .file("Bar.java").withId("Bar.java")
                        .clazz("Bar.java")
                        .endInFile()
                    .end()
                    .file("Bing.java").withId("Bing.java")
                        .clazz("Bing.java")
                        .endInFile()
                    .end()
                .end()
            .end()
        return modelBuilder
    }
}
