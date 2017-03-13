package com.atlassian.clover.ant.tasks

import com.atlassian.clover.instr.java.RecorderInstrEmitter
import com.atlassian.clover.api.CloverException
import com.atlassian.clover.remote.DistributedConfig

class CloverInstrTaskTest extends CloverBuildFileTestBase {

    CloverInstrTaskTest(String aTestName) {
        super(aTestName)
    }

    String getAntFileName() {
        return "clover-instr.xml"
    }

    void testSimpleFileset() {
        expectLogContaining("simpleFileset", "Clover all over. Instrumented 5 files")
    }

    void testSimpleSrc() {
        expectLogContaining("simpleSrc", "Clover all over. Instrumented 5 files")
        expectLogContaining("simpleSrcLongName", "Clover all over. Instrumented 5 files")
    }

    void testSimpleTestSrc() {
        expectLogContaining("simpleTestSrc", "Clover all over. Instrumented 5 files")
        assertTrue(getFullLog().contains("26 test methods detected"))
    }

    void testIntersectingTestSrc() {
        expectLogContaining("intersectingTestSrc", "Clover all over. Instrumented 5 files")
        assertTrue(getFullLog().contains("27 test methods detected"))
    }

    void testSimpleTestFileSet() {
        expectLogContaining("simpleTestFileSet", "Clover all over. Instrumented 2 files")
        assertTrue(getFullLog().contains("27 test methods detected"))
    }

    void testCustomTestFileSet() {
        expectLogContaining("customTestFileSet", "Clover all over. Instrumented 2 files")
        assertTrue(getFullLog().contains("9 test methods detected"))
    }

    void testCustomTestFileSetRegExError() {
        expectBuildExceptionContaining("customTestFileSetBadRE", "BuildException", "Error parsing regular expression")
    }
    
    void testContextDefs() {
        expectLogContaining("contextDefs", "Clover all over. Instrumented 5 files")
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