package com.atlassian.clover.eclipse.core.views.testcontributions;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import com.atlassian.clover.registry.entities.TestCaseInfo;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

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
    
    public Set<FullClassInfo> getAllTestClasses() {
        Set<FullClassInfo> testClasses = new HashSet<>(testCases.size());
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
