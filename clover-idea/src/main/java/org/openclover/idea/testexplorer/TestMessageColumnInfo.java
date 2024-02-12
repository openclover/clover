package org.openclover.idea.testexplorer;

import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.idea.treetables.AbstractColumnInfo;
import org.openclover.idea.util.ComparatorUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public class TestMessageColumnInfo extends AbstractColumnInfo<String> {
    public TestMessageColumnInfo() {
        super("Message");
    }

    @Override
    public String valueOf(DefaultMutableTreeNode node) {
        final Object userObject = node.getUserObject();
        if (userObject instanceof TestCaseInfo) {
            TestCaseInfo tci = (TestCaseInfo) userObject;
            return tci.getFailMessage();
        } else {
            return null;
        }
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return COMPARATOR;
    }

    private static final Comparator<DefaultMutableTreeNode> COMPARATOR = new AbstractTestCaseNodeComparator() {
        @Override
        int compare(TestCaseInfo tci1, TestCaseInfo tci2) {
            final String value1 = tci1.getFailMessage();
            final String value2 = tci2.getFailMessage();

            return ComparatorUtil.compare(value1, value2);
        }
    };

}
