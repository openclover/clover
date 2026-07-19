package org.openclover.core.cfg.instr

import org.junit.Test
import org.openclover.core.instr.tests.DefaultTestDetector
import org.openclover.core.instr.tests.FileMappedTestDetector
import org.openclover.core.instr.tests.NoTestDetector
import org.openclover.core.instr.tests.SimpleTestSourceMatcher
import org.openclover.core.instr.tests.TestSpec
import org.openclover.runtime.remote.DistributedConfig
import org_openclover_runtime.CloverProfile

import java.util.regex.Pattern

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

class InstrumentationConfigSerializationTest {

    private static InstrumentationConfig saveAndLoad(InstrumentationConfig config) {
        File tmp = File.createTempFile("clover-test", "tmp")
        tmp.deleteOnExit()
        config.saveToFile(tmp)
        return tmp.withInputStream { InstrumentationConfig.loadFromStream(it) }
    }

    /**
     * This simply ensures the object tree is serializable.
     */
    @Test
    void testSerializeSanity() throws IOException {
        InstrumentationConfig config = new InstrumentationConfig()
        config.setDistributedConfig(new DistributedConfig("foo=bar"))

        File tmp = File.createTempFile("clover-test", "tmp")
        tmp.deleteOnExit()

        config.saveToFile(tmp)
    }

    @Test
    void testScalarAndStringFieldsRoundTrip() {
        InstrumentationConfig config = new InstrumentationConfig()
        config.setEnabled(false)
        config.setFlushPolicy(InstrumentationConfig.INTERVAL_FLUSHING)
        config.setFlushInterval(1234)
        config.setSliceRecording(false)
        config.setReportInitErrors(false)
        config.setRecordTestResults(false)
        config.setInstrLevel(InstrumentationLevel.METHOD.ordinal())
        config.setRelative(true)
        config.setInitstring("path/to/clover.db")
        config.setProjectName("MyProject")
        config.setEncoding("UTF-8")
        config.setRegistryFile(new File("some/registry.db"))
        config.setDefaultBaseDir(new File("base"))
        config.setTmpDir(new File("tmp"))
        config.setIncludedFiles([new File("A.java"), new File("B.java")])

        InstrumentationConfig read = saveAndLoad(config)

        assertFalse(read.isEnabled())
        assertEquals(InstrumentationConfig.INTERVAL_FLUSHING, read.getFlushPolicy())
        assertEquals(1234, read.getFlushInterval())
        assertFalse(read.isSliceRecording())
        assertFalse(read.isReportInitErrors())
        assertFalse(read.isRecordTestResults())
        assertEquals(InstrumentationLevel.METHOD.ordinal(), read.getInstrLevel())
        assertTrue(read.isRelative())
        assertEquals("path/to/clover.db", read.getInitString())
        assertEquals("MyProject", read.getProjectName())
        assertEquals("UTF-8", read.getEncoding())
        assertEquals(new File("some/registry.db"), read.getRegistryFile())
        assertEquals(new File("base"), read.getDefaultBaseDir())
        assertEquals(new File("tmp"), read.getTmpDir())
        assertEquals([new File("A.java"), new File("B.java")] as Set, read.getIncludedFiles() as Set)
    }

    @Test
    void testNullOptionalFieldsRoundTrip() {
        InstrumentationConfig read = saveAndLoad(new InstrumentationConfig())

        assertNull(read.getInitString())
        assertNull(read.getEncoding())
        assertNull(read.getIncludedFiles())
        assertNull(read.getDistributedConfigString())
        assertNull(read.getMethodContexts())
        assertNull(read.getStatementContexts())
        assertNull(read.getProfiles())
        // testDetector defaults to a DefaultTestDetector lazily
        assertTrue(read.getTestDetector() instanceof DefaultTestDetector)
    }

    @Test
    void testDistributedConfigRoundTrips() {
        InstrumentationConfig config = new InstrumentationConfig()
        config.setDistributedConfig(new DistributedConfig("host=ahost;port=123;timeout=555;numClients=2;retryPeriod=1000"))

        InstrumentationConfig read = saveAndLoad(config)

        assertEquals(config.getDistributedConfigString(), read.getDistributedConfigString())
    }

    @Test
    void testContextDefsRoundTrip() {
        InstrumentationConfig config = new InstrumentationConfig()
        MethodContextDef mcd = new MethodContextDef()
        mcd.setName("getters")
        mcd.setRegexp("public.*get.*")
        mcd.setMaxComplexity(3)
        mcd.setMaxStatements(5)
        config.addMethodContext(mcd)
        config.addStatementContext(new StatementContextDef("logging", "log\\..*"))

        InstrumentationConfig read = saveAndLoad(config)

        assertEquals(1, read.getMethodContexts().size())
        assertEquals("getters", read.getMethodContexts()[0].getName())
        assertEquals("public.*get.*", read.getMethodContexts()[0].getRegexp())
        assertEquals(3, read.getMethodContexts()[0].getMaxComplexity())
        assertEquals(5, read.getMethodContexts()[0].getMaxStatements())
        assertEquals(1, read.getStatementContexts().size())
        assertEquals("logging", read.getStatementContexts()[0].getName())
        assertEquals("log\\..*", read.getStatementContexts()[0].getRegexp())
    }

    @Test
    void testProfilesRoundTrip() {
        InstrumentationConfig config = new InstrumentationConfig()
        config.addProfile(new CloverProfile("default", CloverProfile.CoverageRecorderType.FIXED, null))
        config.addProfile(new CloverProfile("dist", CloverProfile.CoverageRecorderType.SHARED,
                new DistributedConfig("host=ahost;port=1;timeout=1;numClients=1;retryPeriod=1")))

        InstrumentationConfig read = saveAndLoad(config)

        assertEquals(2, read.getProfiles().size())
        assertEquals("default", read.getProfiles()[0].getName())
        assertEquals(CloverProfile.CoverageRecorderType.FIXED, read.getProfiles()[0].getCoverageRecorder())
        assertNull(read.getProfiles()[0].getDistributedCoverage())
        assertEquals("dist", read.getProfiles()[1].getName())
        assertEquals(CloverProfile.CoverageRecorderType.SHARED, read.getProfiles()[1].getCoverageRecorder())
        assertEquals("ahost", read.getProfiles()[1].getDistributedCoverage().getHost())
    }

    /**
     * Reproduces the shape produced by the Ant path (GroovycSupport) and consumed by
     * the Groovy transformer: a config carrying a {@link FileMappedTestDetector} whose
     * matchers are resolved {@link SimpleTestSourceMatcher}s. Asserts the detector
     * survives the save/load boundary and still detects tests.
     */
    @Test
    void testAntToGroovyTestDetectorRoundTrip() {
        TestSpec spec = new TestSpec()
        spec.setClassPattern(Pattern.compile(".*Test"))

        FileMappedTestDetector fileMapped = new FileMappedTestDetector(new NoTestDetector())
        fileMapped.addTestSourceMatcher(
                new SimpleTestSourceMatcher([new File("test/com/foo/FooTest.java")] as Set, spec))

        InstrumentationConfig config = new InstrumentationConfig()
        config.setTestDetector(fileMapped)

        InstrumentationConfig read = saveAndLoad(config)

        assertTrue(read.getTestDetector() instanceof FileMappedTestDetector)
        FileMappedTestDetector readDetector = (FileMappedTestDetector) read.getTestDetector()
        assertEquals(1, readDetector.@testFileMatchers.size())
        assertTrue(readDetector.@testFileMatchers[0] instanceof SimpleTestSourceMatcher)
        assertTrue(readDetector.@testFileMatchers[0].matchesFile(new File("test/com/foo/FooTest.java")))
        assertTrue(readDetector.@defaultDetector instanceof NoTestDetector)
        assertEquals(".*Test", readDetector.@testFileMatchers[0].getDetector().@classPattern.pattern())
    }
}
