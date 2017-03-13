package com.atlassian.clover.ant.tasks

class CloverOptimizeJUnitSrcFilesetFuncTest extends CloverOptimizeJUnitFilesetTestBase {
    CloverOptimizeJUnitSrcFilesetFuncTest(String name) {
        super(
            name,
            "runJUnitSourceFileSetTests",
            [
                "testOptimizedCIBuildCyclesWithAlwaysRunTests" : "runJUnitSourceFileSetTestsWithAlwaysRunTests",
                "testOptimizedCIBuildCyclesWithMixedOptimizedTests" : "runJUnitSourceFileSetTestsWithMixedOptimizedTests"
            ])
    }

}
