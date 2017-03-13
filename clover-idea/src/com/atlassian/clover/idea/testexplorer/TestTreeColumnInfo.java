package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.idea.treetables.MyTreeColumnInfo;
import com.atlassian.clover.idea.util.ComparatorUtil;
import com.atlassian.clover.registry.entities.TestCaseInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public class TestTreeColumnInfo extends MyTreeColumnInfo {

    public TestTreeColumnInfo() {
        super("Test Case");
    }

    @Override
    public String getPreferredStringValue() {
        return "XXXXXXXXXXXXXXXXXXXXXXXXX";
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return COMPARATOR;
    }

    private final Comparator<DefaultMutableTreeNode> COMPARATOR = new AbstractTestCaseNodeComparator() {
        @Override
        public int compare(TestCaseInfo tci1, TestCaseInfo tci2) {
            final String value1 = tci1.getTestName();
            final String value2 = tci2.getTestName();

            return ComparatorUtil.compare(value1, value2);
        }
    };

}
