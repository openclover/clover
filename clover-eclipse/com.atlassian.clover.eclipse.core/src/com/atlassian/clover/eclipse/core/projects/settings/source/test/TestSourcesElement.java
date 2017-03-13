package com.atlassian.clover.eclipse.core.projects.settings.source.test;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class TestSourcesElement implements TreeElement {
    private List<TestPackageRootElement> testPackageRoots = newArrayList();

    public List<TestPackageRootElement> getTestPackageRoots() {
        return testPackageRoots;
    }

    public void removeTestPackageRoot(TestPackageRootElement testPackageRoot) {
        testPackageRoots.remove(testPackageRoot);
    }

    @Override
    public TreeElement getParent() {
        return null;
    }

    public TestPackageRootElement newTestPackageRoot(String path) {
        TestPackageRootElement testPackageRoot = new TestPackageRootElement(this, path);
        testPackageRoots.add(testPackageRoot);
        return testPackageRoot;
    }
}
