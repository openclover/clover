package com.atlassian.clover.ant.tasks

import groovy.transform.CompileStatic

@CompileStatic
abstract class CloverOptimizeJUnitFilesetTestBase extends CloverOptimizeJUnitTestBase {
    CloverOptimizeJUnitFilesetTestBase(String name, String defaultRunTarget, Map runTargetsForTests) {
        super(name, defaultRunTarget, runTargetsForTests)
    }

    void testOptimizedCIBuildCyclesWithAlwaysRunTests() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : [ "AppClass2", cycle(0) ],
                    "AppClass23Test" :  [ "AppClass2", cycle(0), "AppClass3", cycle(0) ],
                    "AppClass234Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0) ],
                    "NoAppClassTest" : [ ] as List<String>
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1)

        expectTestsRunResults(
            cycle(1),
            [
                    "NoAppClassTest" : [] as List<String>
            ])

        buildComplete()
        sourceChange()
        buildThenRun(2)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : [ "AppClass2", cycle(2) ],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0) ],
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0) ],
                    "NoAppClassTest" :  [ ] as List<String>
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3) ],
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0) ],
                    "NoAppClassTest" : [ ] as List<String>
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4) ],
                    "NoAppClassTest" : [ ] as List<String>
            ])
    }

    void testTestMinimizationWithFastFailOrdering() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        //No ordering for the first run
        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : [ "AppClass2", cycle(0) ],
                    "AppClass23Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0) ],
                    "AppClass234Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0) ],
                    "NoAppClassTest" : [] as List<String>
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1)

        expectNoTestRunResults(cycle(1))

        buildComplete()
        sourceChange()
        buildThenRun(2)

        expectOrderedTestsRunResults(
            cycle(2),
            [
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0) ],
                    "AppClass2Test" : [ "AppClass2", cycle(2) ],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0) ]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3)

        expectOrderedTestsRunResults(
            cycle(3),
            [
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0) ],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3) ]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4)

        expectOrderedTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4) ]
            ])
    }

    /**
     * Duration order: AppClass234Test > NoAppClassTest > AppClass2Test > AppClass23Test > AlwaysFailingTest
     * AlwaysFailingTest does what it says so should always run first
     * Changes made to AppClass2, AppClass3 and AppClass4 in cycles 2, 3, 4 respectively
     */
    void testNoTestMinimizationWithFastFailOrdering() throws Exception {
        initialSourceCreation()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(0)

        //No ordering for the first run
        expectTestsRunResults(
            cycle(0),
            [
                    "AlwaysFailingTest" : [ ] as List<String>,
                    "AppClass2Test" : [ "AppClass2", cycle(0) ],
                    "AppClass23Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0) ],
                    "AppClass234Test"  : [ "AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0) ],
                    "NoAppClassTest" : [ ]  as List<String>
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(1)

        expectOrderedTestsRunResults(
            cycle(1),
            [
                    "AlwaysFailingTest" : [ ] as List<String>,
                    "AppClass234Test" :  [ "AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0) ],
                    "NoAppClassTest" : [ ] as List<String>,
                    "AppClass2Test" : [ "AppClass2", cycle(0) ],
                    "AppClass23Test" : [ "AppClass2", cycle(0), "AppClass3", cycle(0) ]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(2)

        expectOrderedTestsRunResults(
            cycle(2),
            [
                    "AlwaysFailingTest" : [ ] as List<String>,
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0) ],
                    "AppClass2Test" : [ "AppClass2", cycle(2) ],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(0) ],
                    "NoAppClassTest" : [ ] as List<String>,
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(3)

        expectOrderedTestsRunResults(
            cycle(3),
            [
                    "AlwaysFailingTest" : [ ] as List<String>,
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0) ],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3) ],
                    "NoAppClassTest" : [ ] as List<String>,
                    "AppClass2Test" : [ "AppClass2", cycle(2) ]
            ])

        buildComplete()
        sourceChange()
        getProject().setProperty("minimize.tests", "false")
        buildThenRun(4)

        expectOrderedTestsRunResults(
            cycle(4),
            [
                    "AlwaysFailingTest" : [ ] as List<String>,
                    "AppClass234Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4) ],
                    "NoAppClassTest" : [ ] as List<String>,
                    "AppClass2Test" : [ "AppClass2", cycle(2) ],
                    "AppClass23Test" : [ "AppClass2", cycle(2), "AppClass3", cycle(3) ]
            ])
    }

    protected void expectOrderedTestsRunResults(String cycle, Map<String, List<String>> testsAndExpectations) throws Exception {
        expectTestsRunResults(cycle, testsAndExpectations)

        File testOrderLog = new File(new File(util.getWorkDir().getAbsolutePath(), cycle), "testorder.log")
        BufferedReader br = new BufferedReader(new FileReader(testOrderLog))
        List<String> lines = new ArrayList<>()
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            lines.add(line)
        }
        assertEquals("Test ordering was not as expected for cycle " + cycle + ", ",
                new ArrayList<String>(testsAndExpectations.keySet()), lines)
    }
}