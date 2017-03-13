package com.atlassian.clover.eclipse.core.projects.settings.source.test;

import com.atlassian.clover.spec.instr.test.BooleanSpec;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public abstract class ExpressionElement implements TreeElement {
    private TreeElement parent;

    final List<TestClassElement> testClasses = newArrayList();
    
    public ExpressionElement(TreeElement parent) {
        this.parent = parent;
    }

    public TestClassElement newTestClassElement() {
        TestClassElement testClass = new TestClassElement(this);
        testClasses.add(testClass);
        return testClass;
    }

    public List getTestClasses() {
        return testClasses;
    }

    public BooleanSpec getSpec() {
        final BooleanSpec booleanSpec = newSpec();
        for (TestClassElement testClass : testClasses) {
            booleanSpec.addConfiguredTestClass(testClass.toTestClassSpec());
        }
        return booleanSpec;
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }

    protected abstract BooleanSpec newSpec();
}
