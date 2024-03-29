package org.openclover.eclipse.core.views.testcontributions;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullClassInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestCaseInfoProvider implements ITreeContentProvider {
    protected Set<TestCaseInfo> testCases = Collections.emptySet();

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        TestContributionsComputation contributionComputation = (TestContributionsComputation)newInput;
        if (contributionComputation != null) {
            testCases = contributionComputation.getTestCases();
        }
    }

    @Override
    public void dispose() { }

    @Override
    public Object[] getChildren(Object object) {
        if (object instanceof TestCaseInfo) {
            return new Object[] {};
        } else {
            return getTestsFor((ClassInfo)object).toArray();
        }
    }

    @Override
    public Object getParent(Object object) {
        if (object instanceof TestCaseInfo) {
            return ((TestCaseInfo)object).getRuntimeType();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasChildren(Object object) {
        return object instanceof ClassInfo;
    }

    @Override
    public Object[] getElements(Object parent) {
        return getAllTestClasses().toArray();
    }

    public Set<TestCaseInfo> getAllTests() {
        return testCases;
    }
    
    public Set<ClassInfo> getAllTestClasses() {
        Set<ClassInfo> testClasses = new HashSet<>(testCases.size());
        for (TestCaseInfo testCase : testCases) {
            if (testCase.isResolved()) {
                testClasses.add(testCase.getRuntimeType());
            }
        }
        return testClasses;
    }

    public Set<TestCaseInfo> getTestsFor(ClassInfo testClass) {
        Set<TestCaseInfo> testClasses = new HashSet<>(testCases.size());
        for (TestCaseInfo testCase : testCases) {
            if (testCase.getRuntimeType() == testClass) {
                testClasses.add(testCase);
            }
        }
        return testClasses;
    }
}
