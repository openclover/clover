package org.openclover.idea.testexplorer;

import org.openclover.idea.treetables.AbstractColumnInfo;
import org.openclover.idea.util.ComparatorUtil;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.util.Formatting;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;
import java.util.Date;

public class TestStartedColumnInfo extends AbstractColumnInfo<String> {
    public TestStartedColumnInfo() {
        super("Started", RALIGN_CELL_RENDERER);
    }

    @Override
    public String valueOf(DefaultMutableTreeNode node) {
        final Object userObject = node.getUserObject();
        if (userObject instanceof TestCaseInfo) {
            TestCaseInfo tci = (TestCaseInfo) userObject;
            return Formatting.formatShortDate(new Date(tci.getStartTime()));
        } else {
            return null;
        }
    }

    @Override
    public String getPreferredStringValue() {
        return "XX XXX XX:XX:XX";
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return COMPARATOR;
    }

    private static final Comparator<DefaultMutableTreeNode> COMPARATOR = new AbstractTestCaseNodeComparator() {
        @Override
        int compare(TestCaseInfo tci1, TestCaseInfo tci2) {
            final long time1 = tci1.getStartTime();
            final long time2 = tci2.getStartTime();

            return ComparatorUtil.compareLong(time1, time2);
        }
    };

}
