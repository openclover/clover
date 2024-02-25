package org.openclover.eclipse.testopt.editors.ruler.tree;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.eclipse.core.views.nodes.Nodes;

public class TestNameLabelProvider extends ColumnLabelProvider {

    @Override
    public String getText(Object element) {
        return element instanceof TestCaseInfo ? ((TestCaseInfo)element).getTestName() : element.toString();
    }
    
    @Override
    public Image getImage(Object element) {
        if (element instanceof TestCaseInfo) {
            final TestCaseInfo tci = (TestCaseInfo) element;
            return Nodes.iconFor(tci);
        } else {
            return CloverPlugin.getImage(CloverPluginIcons.TEST_CLASS_ICON);
        }
    }
}
