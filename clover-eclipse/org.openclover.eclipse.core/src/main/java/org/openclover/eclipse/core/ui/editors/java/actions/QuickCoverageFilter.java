package org.openclover.eclipse.core.ui.editors.java.actions;

import org.eclipse.jdt.internal.ui.text.AbstractInformationControl;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.views.testcontributions.TestCaseInfoLabelProvider;
import org.openclover.eclipse.core.views.testcontributions.TestCaseInfoProvider;

public class QuickCoverageFilter extends AbstractInformationControl {

    private Tree tree;
    private CheckboxTreeViewer treeViewer;
    private TestCaseInfoProvider allTestCaseInfoProvider;

    public QuickCoverageFilter(Shell parent, int shellStyle, int treeStyle, String invokingCommandId, boolean showStatusField) {
        super(parent, shellStyle, treeStyle, invokingCommandId, showStatusField);
    }

    @Override
    protected TreeViewer createTreeViewer(Composite parent, int style) {
        tree =
            new Tree(
                parent,
                SWT.CHECK | style);
        tree.setHeaderVisible(true);

        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText("Test");
        column.setWidth(200);

        treeViewer = new CheckboxTreeViewer(tree);
//      allTestCaseInfoProvider = new AllTestCaseInfoProvider();
        treeViewer.setContentProvider(allTestCaseInfoProvider);
        treeViewer.setLabelProvider(new TestCaseInfoLabelProvider());
//      treeViewer.addCheckStateListener(allTestCaseInfoProvider);
        treeViewer.setAutoExpandLevel(2);

        return treeViewer;
    }

    @Override
    public void setInput(Object input) {
        treeViewer.setInput(input);
//      treeViewer.setCheckedElements(allTestCaseInfoProvider.getCheckedItems());
        treeViewer.refresh();
    }

    @Override
    protected String getId() {
        return CloverPlugin.ID + ".editors.java.action.QuickCoverageFilter";
    }
}
