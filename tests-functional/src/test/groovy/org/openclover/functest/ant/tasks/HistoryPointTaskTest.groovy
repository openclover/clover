package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic

@CompileStatic
class HistoryPointTaskTest extends CloverBuildFileTestBase {

    HistoryPointTaskTest(String aTestName) {
        super(aTestName)
    }

    String getAntFileName() {
        return "history-point.xml"
    }

    // TODO: asserts
    void testCreateHistoryPoint() {
        executeTarget("testCreateHistoryPoint")
    }

    void testTestSourcesElement() {
        executeTarget("testTestSourcesElement")
    }
    
    void testTestResultsElement() {
        executeTarget("testTestResultsElement")
    }

    void testFilter() {
        executeTarget("testFilter")
    }

    void testOverwrite() {
        executeTarget("testOverwriteTrue")
        expectLogContaining("testOverwriteFalse", "Not overwriting existing history point")
    }
    
    void testSrcLevel() {
        executeTarget("testSrcLevel")
    }
}
