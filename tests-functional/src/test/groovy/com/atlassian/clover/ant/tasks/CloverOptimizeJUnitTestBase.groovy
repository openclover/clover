package com.atlassian.clover.ant.tasks

import com.atlassian.clover.cfg.instr.InstrumentationLevel

import static org.openclover.util.Maps.newHashMap

abstract class CloverOptimizeJUnitTestBase extends CloverOptimizeTestBase {
    protected CloverOptimizeJUnitTestBase(String name, String defaultRunTarget, Map<String, String> runTargetsForTests) {
        super(name, defaultRunTarget, runTargetsForTests)
    }

    protected void _testOptimizedCIBuildCycles() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : [ "AppClass2", cycle(0) ] as String[],
                    "AppClass23Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0) ] as String[],
                    "AppClass234Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0) ] as String[],
                    "NoAppClassTest" : [ ] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1)

        expectNoTestRunResults(cycle(1))

        buildComplete()
        sourceChange()
        buildThenRun(2)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : [ "AppClass2", cycle(2) ] as String[],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0) ] as String[],
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0) ] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3) ] as String[],
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0) ] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4) ] as String[]
            ])
    }

    void testOptimizedCIBuildCycles() throws Exception {
        _testOptimizedCIBuildCycles()
    }

    void testOptimizedCIBuildCyclesWithFailingTest() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AlwaysFailingTest" : [] as String[],
                    "AppClass2Test" : [ "AppClass2", cycle(0) ] as String[],
                    "AppClass23Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0) ] as String[],
                    "AppClass234Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0) ] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1)

        expectTestsRunResults(
            cycle(1),
            [
                    "AlwaysFailingTest" : []
            ])

        buildComplete()
        sourceChange()
        buildThenRun(2)

        expectTestsRunResults(
            cycle(2),
            [
                    "AlwaysFailingTest" : [] as String[],
                    "AppClass2Test" : ["AppClass2", cycle(2) ] as String[],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0) ] as String[],
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0) ] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3)

        expectTestsRunResults(
            cycle(3),
            [
                    "AlwaysFailingTest" : [],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4)

        expectTestsRunResults(
            cycle(4),
            [
                    "AlwaysFailingTest" : [],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)]
            ])
    }

    void xtestOptimizedCIBuildCyclesWithScrubbedCoverage() throws Exception {
        initialSourceCreation()
        buildThenRun(0, true, "cloverCleanCoverage", defaultRunTarget)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1, true, "cloverCleanCoverage", defaultRunTarget)

        expectNoTestRunResults(cycle(1))

        buildComplete()
        sourceChange()
        buildThenRun(2, true, "cloverCleanCoverage", defaultRunTarget)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3, true, "cloverCleanCoverage", defaultRunTarget)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4, true, "cloverCleanCoverage", defaultRunTarget)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[]
            ])
    }

    void testOptimizedCIBuildCyclesWithFrequentUnoptimizedRuns() throws Exception {
        initialSourceCreation()
        getProject().setProperty("max.optimized.builds", "2")
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "AppClass2345Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0), "AppClass5", cycle(0)] as String[],
                    "AppClass23456Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0), "AppClass5", cycle(0), "AppClass6", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        //INCREMENTAL TEST RUN

        buildComplete()
        sourceChange()
        getProject().setProperty("max.optimized.builds", "2")
        buildThenRun(1)

        expectNoTestRunResults(cycle(1))

        //INCREMENTAL TEST RUN

        buildComplete()
        sourceChange()
        getProject().setProperty("max.optimized.builds", "2")
        buildThenRun(2)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "AppClass2345Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0), "AppClass5", cycle(0)] as String[],
                    "AppClass23456Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0), "AppClass5", cycle(0), "AppClass6", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("max.optimized.builds", "2")
        buildThenRun(3)

        //FULL TEST RUN

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)] as String[],
                    "AppClass2345Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0), "AppClass5", cycle(0)] as String[],
                    "AppClass23456Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0), "AppClass5", cycle(0), "AppClass6", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        //INCREMENTAL TEST RUN

        buildComplete()
        sourceChange()
        getProject().setProperty("max.optimized.builds", "2")
        buildThenRun(4)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[],
                    "AppClass2345Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4), "AppClass5", cycle(0)] as String[],
                    "AppClass23456Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4), "AppClass5", cycle(0), "AppClass6", cycle(0)] as String[]
            ])

        //INCREMENTAL TEST RUN

        buildComplete()
        sourceChange()
        getProject().setProperty("max.optimized.builds", "2")
        buildThenRun(5)

        expectTestsRunResults(
            cycle(5),
            [
                    "AppClass2345Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4), "AppClass5", cycle(5)] as String[],
                    "AppClass23456Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4), "AppClass5", cycle(5), "AppClass6", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("max.optimized.builds", "2")
        buildThenRun(6)

        //FULL TEST RUN

        expectTestsRunResults(
            cycle(6),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[],
                    "AppClass2345Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4), "AppClass5", cycle(5)] as String[],
                    "AppClass23456Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4), "AppClass5", cycle(5), "AppClass6", cycle(6)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])
    }

    void testOptimizedCIBuildCyclesWithMixedOptimizedTests() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1)

        expectTestsRunResults(
            cycle(1),
            [
                    "NoAppClassTest" : []
            ])

        buildComplete()
        sourceChange()
        buildThenRun(2)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])
    }

    void testOptimizedCIBuildCyclesWithNoChanges() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        noSourceChange()
        buildThenRun(1, true)

        expectTestsRunResults(
            cycle(1),
            newHashMap())

        buildComplete()
        noSourceChange()
        buildThenRun(2, true)

        expectTestsRunResults(
            cycle(2),
            newHashMap())
    }

    void testUnoptimizedCIBuildCycles() throws Exception {
        initialSourceCreation()
        buildThenRun(0, false)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1, false)

        expectTestsRunResults(
            cycle(1),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(2, false)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3, false)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4, false)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])
    }

    void testOptimizedCIBuildCyclesWithMinimizationTurnedOff() throws Exception {
        initialSourceCreation()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(0, true)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(1, true)

        expectTestsRunResults(
            cycle(1),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(2, false)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(3, false)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(4, false)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])
    }

    void testOptimizedCIBuildCyclesWithFailedTestLaterCorrected() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[],
                    "InitiallyFailingTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1)

        expectTestsRunResults(
            cycle(1),
            [
                    "InitiallyFailingTest" : []
            ])

        buildComplete()
        sourceChange()
        buildThenRun(2)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[]
            ])
    }

    protected String getTestSourceBaseName() {
        return "CloverOptimizeJUnitTest"
    }

    protected static Map<String, String> methodLevelInstr(Map<String, String> properties) {
        properties.put("instrumentation.level", InstrumentationLevel.METHOD.name().toLowerCase())
        return properties
    }
}