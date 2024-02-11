package org.openclover.eclipse.core.projects.settings.source.test;

import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

public class TestMethodsElement implements TreeElement {
    private TestClassElement parent;
    private List<TestMethodElement> methods = newArrayList();

    public TestMethodsElement(TestClassElement parent) {
        this.parent = parent;
    }

    public List<TestMethodElement> getMethods() {
        return methods;
    }

    public TestMethodElement newTestMethod(String name, String annotation, String returnType) {
        TestMethodElement testMethod = new TestMethodElement(this, name, annotation, returnType);
        methods.add(testMethod);
        return testMethod;
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }
}
