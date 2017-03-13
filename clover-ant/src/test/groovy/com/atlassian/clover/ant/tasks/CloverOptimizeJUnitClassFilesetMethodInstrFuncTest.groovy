package com.atlassian.clover.ant.tasks

class CloverOptimizeJUnitClassFilesetMethodInstrFuncTest extends CloverOptimizeJUnitClassFilesetFuncTest {
    CloverOptimizeJUnitClassFilesetMethodInstrFuncTest(String name) {
        super(name)
    }

    protected Map<String, String> newProjectProperties() {
        return methodLevelInstr(super.newProjectProperties())
    }
}
