package org.openclover.eclipse.core.views.testcontributions;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;

public class AllTestCaseInfoProvider extends TestCaseInfoProvider {
    public static final Object ALL_TEST_CASES = new Object();

    @Override
    public Object[] getChildren(Object object) {
        if (object instanceof TestCaseInfo) {
            return new Object[] {};
        } else if (object instanceof ClassInfo) {
            return getTestsFor((ClassInfo)object).toArray();
        } else {
            return getAllTestClasses().toArray();
        }
    }

    @Override
    public Object getParent(Object object) {
        if (object instanceof TestCaseInfo) {
            return ((TestCaseInfo)object).getRuntimeType();
        } else if (object == ALL_TEST_CASES) {
            return null;
        } else {
            return ALL_TEST_CASES; 
        }
    }

    @Override
    public boolean hasChildren(Object object) {
        return object == ALL_TEST_CASES || object instanceof ClassInfo;
    }

    @Override
    public Object[] getElements(Object parent) {
        return new Object[] {ALL_TEST_CASES};
    }
}
