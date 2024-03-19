package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic
import org.openclover.core.instr.java.RecorderInstrEmitter
import org.openclover.runtime.api.CloverException
import org.openclover.runtime.remote.DistributedConfig

@CompileStatic
class CloverInstrTaskTest extends CloverBuildFileTestBase {

    CloverInstrTaskTest(String aTestName) {
        super(aTestName)
    }

    String getAntFileName() {
        return "clover-instr.xml"
    }

    void testSimpleFileset() {
        expectLogContaining("simpleFileset", "OpenClover instrumented 5 files")
    }

    void testSimpleSrc() {
        expectLogContaining("simpleSrc", "OpenClover instrumented 5 files")
        expectLogContaining("simpleSrcLongName", "OpenClover instrumented 5 files")
    }

    void testSimpleTestSrc() {
        expectLogContaining("simpleTestSrc", "OpenClover instrumented 5 files")
        assertTrue(getFullLog().contains("26 test methods detected"))
    }

    void testIntersectingTestSrc() {
        expectLogContaining("intersectingTestSrc", "OpenClover instrumented 5 files")
        assertTrue(getFullLog().contains("27 test methods detected"))
    }

    void testSimpleTestFileSet() {
        expectLogContaining("simpleTestFileSet", "OpenClover instrumented 2 files")
        assertTrue(getFullLog().contains("27 test methods detected"))
    }

    void testCustomTestFileSet() {
        expectLogContaining("customTestFileSet", "OpenClover instrumented 2 files")
        assertTrue(getFullLog().contains("9 test methods detected"))
    }

    void testCustomTestFileSetRegExError() {
        expectBuildExceptionContaining("customTestFileSetBadRE", "BuildException", "Error parsing regular expression")
    }
    
    void testContextDefs() {
        expectLogContaining("contextDefs", "OpenClover instrumented 5 files")
        assertTrue(getFullLog().contains("Method context match, line 53, id=property"))
        assertTrue(getFullLog().contains("Method context match, line 74, id=toString"))
        assertTrue(getFullLog().contains("Method context match, line 74, id=lowCmp"))
    }

    void testSrcEqualsDest() {
        expectBuildExceptionContaining("errorSrcEqualsDest1", "BuildException", "srcdir cannot be the same as destdir")
        expectBuildExceptionContaining("errorSrcEqualsDest2", "BuildException", "srcdir cannot be the same as destdir")
    }

    void testMissingDestDir() {
        expectBuildException("errorMissingDestDir", "destdir is required")
    }

    void testBadSrcDir() {
        expectBuildExceptionContaining("errorBadSrcDir", "BuildException", " not found")
    }

    void testRelativeTrue() {
        executeTarget("relativeTrue")
        assertPropertySet("relative.initstring")
    }

    void testRelativeFalse() {
        executeTarget("relativeFalse")
        assertPropertyUnset("absolute.initstring")
    }

    void testDefaultRelativeInitString() {
        executeTarget("defaultRelativeInitString")
        assertPropertySet("relative.default.initstring")
    }

    void testDistributedConfiguration() throws CloverException {
        DistributedConfig conf = new DistributedConfig()
        conf.setHost("myhost")
        conf.setPort(1111)
        conf.setNumClients(2)
        conf.setTimeout(1000)
        getProject().setProperty("expected.dist.config", RecorderInstrEmitter.asUnicodeString(conf.toString()))
        executeTarget("distributedConfiguration")
        assertPropertySet("distributed.configuration")
    }
}