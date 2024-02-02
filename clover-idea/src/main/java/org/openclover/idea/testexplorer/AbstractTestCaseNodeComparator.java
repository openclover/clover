package org.openclover.idea.testexplorer;

import com.atlassian.clover.registry.entities.TestCaseInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public abstract class AbstractTestCaseNodeComparator implements Comparator<DefaultMutableTreeNode> {
    @Override
    public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
        final Object userObject1 = o1.getUserObject();
        final Object userObject2 = o2.getUserObject();

        final TestCaseInfo tci1 = userObject1 instanceof TestCaseInfo ? (TestCaseInfo) userObject1 : null;
        final TestCaseInfo tci2 = userObject2 instanceof TestCaseInfo ? (TestCaseInfo) userObject2 : null;

        if (tci1 == tci2) {
            return 0;
        }

        if (tci1 == null) {
            return -1;
        }

        if (tci2 == null) {
            return 1;
        }

        return compare(tci1, tci2);
    }

    abstract int compare(TestCaseInfo tci1, TestCaseInfo tci2);
}
