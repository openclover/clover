package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic

@CompileStatic
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
