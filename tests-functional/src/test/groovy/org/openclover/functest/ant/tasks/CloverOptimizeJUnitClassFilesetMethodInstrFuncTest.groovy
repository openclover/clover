package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic

@CompileStatic
class CloverOptimizeJUnitClassFilesetMethodInstrFuncTest extends CloverOptimizeJUnitClassFilesetFuncTest {
    CloverOptimizeJUnitClassFilesetMethodInstrFuncTest(String name) {
        super(name)
    }

    protected Map<String, String> newProjectProperties() {
        return methodLevelInstr(super.newProjectProperties())
    }
}
