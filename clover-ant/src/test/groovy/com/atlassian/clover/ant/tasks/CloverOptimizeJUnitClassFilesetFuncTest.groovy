package com.atlassian.clover.ant.tasks

class CloverOptimizeJUnitClassFilesetFuncTest extends CloverOptimizeJUnitFilesetTestBase {
    CloverOptimizeJUnitClassFilesetFuncTest(String name) {
        super(
            name,
            "runJUnitClassFileSetTests",
            [
                "testOptimizedCIBuildCyclesWithAlwaysRunTests" : "runJUnitClassFileSetTestsWithAlwaysRunTests",
                "testOptimizedCIBuildCyclesWithMixedOptimizedTests" :  "runJUnitClassFileSetTestsWithMixedOptimizedTests"
            ])
    }

}
