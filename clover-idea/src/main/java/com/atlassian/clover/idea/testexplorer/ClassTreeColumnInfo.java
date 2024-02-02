package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.idea.treetables.MyTreeColumnInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public class ClassTreeColumnInfo extends MyTreeColumnInfo {

    public ClassTreeColumnInfo() {
        super("Element");
    }

    @Override
    public String getPreferredStringValue() {
        return "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return COMPARATOR;
    }

    private final Comparator<DefaultMutableTreeNode> COMPARATOR = (o1, o2) -> {
        final Object uo1 = o1.getUserObject();
        final Object uo2 = o2.getUserObject();

        final CoverageDataHolder value1 = uo1 instanceof CoverageDataHolder ? (CoverageDataHolder) uo1 : null;
        final CoverageDataHolder value2 = uo2 instanceof CoverageDataHolder ? (CoverageDataHolder) uo2 : null;

        // compare only CoverageDataHolder's - leave other in place (pscakge should always be first)
        return (value1 == null || value2 == null) ?
                0 : value1.getElement().getName().compareTo(value2.getElement().getName());
    };
}
