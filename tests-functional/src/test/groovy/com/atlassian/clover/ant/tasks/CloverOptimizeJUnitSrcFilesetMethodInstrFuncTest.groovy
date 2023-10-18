package com.atlassian.clover.ant.tasks

import groovy.transform.CompileStatic

@CompileStatic
class CloverOptimizeJUnitSrcFilesetMethodInstrFuncTest extends CloverOptimizeJUnitSrcFilesetFuncTest {
    CloverOptimizeJUnitSrcFilesetMethodInstrFuncTest(String name) {
        super(name)
    }

    protected Map<String, String> newProjectProperties() {
        return methodLevelInstr(super.newProjectProperties())
    }
}
