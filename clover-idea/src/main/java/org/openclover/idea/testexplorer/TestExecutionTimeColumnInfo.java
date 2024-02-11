package org.openclover.idea.testexplorer;

import org.openclover.idea.treetables.AbstractColumnInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.runtime.util.Formatting;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public class TestExecutionTimeColumnInfo extends AbstractColumnInfo<String> {
    public TestExecutionTimeColumnInfo() {
        super("Time", RALIGN_CELL_RENDERER);
    }

    @Override
    public String valueOf(DefaultMutableTreeNode node) {
        final Object userObject = node.getUserObject();
        if (userObject instanceof TestCaseInfo) {
            TestCaseInfo tci = (TestCaseInfo) userObject;
            return Formatting.format3d(tci.getDuration()) + "s";
        } else {
            return null;
        }
    }

    @Override
    public String getPreferredStringValue() {
        return "000 000.000s";
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return COMPARATOR;
    }

    private static final Comparator<DefaultMutableTreeNode> COMPARATOR = new AbstractTestCaseNodeComparator() {
        @Override
        int compare(TestCaseInfo tci1, TestCaseInfo tci2) {
            return Double.compare(tci1.getDuration(), tci2.getDuration());
        }
    };

}
