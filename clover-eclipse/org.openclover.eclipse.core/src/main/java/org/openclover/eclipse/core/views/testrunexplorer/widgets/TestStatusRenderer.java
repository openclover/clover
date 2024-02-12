package org.openclover.eclipse.core.views.testrunexplorer.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.testrunexplorer.TestRunExplorerViewSettings;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.TestCaseNode;
import org.openclover.eclipse.core.views.widgets.BaseListeningRenderer;

public class TestStatusRenderer extends BaseListeningRenderer {
    @Override
    protected void erase(Event event) {
        lastRowHeight = event.height;
        lastRowX = event.x;
        lastRowY = event.y;
        if (forThisColumn(event)) {
            if (isForTestCaseNode(event)) {
                Color oldBackground = event.gc.getBackground();

                Color newBackground =
                    ((TestCaseNode)event.item.getData()).getStatus() == TestCaseNode.STATUS_PASS
                        ? event.gc.getDevice().getSystemColor(SWT.COLOR_GREEN)
                        : event.gc.getDevice().getSystemColor(SWT.COLOR_RED);

                event.gc.setBackground(newBackground);

                event.gc.fillRectangle(event.x, event.y, event.width, event.height);

                event.gc.setBackground(oldBackground);
                event.detail &= ~SWT.BACKGROUND;
                event.detail &= ~SwtUtils.SWT_HOT;
            } else if (forSelection(event)) {
                SwtUtils.renderSelectionBackground(event);
                event.detail &= ~SWT.BACKGROUND;
                event.detail &= ~SwtUtils.SWT_HOT;
            }
        }
    }

    @Override
    protected void paint(Event event) {
       if (shouldRenderForeground(event)) {
           Object element = event.item.getData();
           Color foreground = event.gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
           String label;
           if (element instanceof TestCaseNode) {
               TestCaseNode testCaseNode = (TestCaseNode)element;
               if (testCaseNode.getStatus() == TestCaseNode.STATUS_PASS) {
                   label = "PASS";
               } else if (testCaseNode.getStatus() == TestCaseNode.STATUS_FAIL) {
                   label = "FAIL";
               } else {
                   label = "ERROR";
               }
           } else {
               label = "";
           }
           SwtUtils.drawText(
               label, event.gc, foreground,
               lastRowX, lastRowY, calcTargetColumnCurrentWidth(event), lastRowHeight,
               MARGIN, SWT.CENTER);
       }
    }

    public TestStatusRenderer(TestRunExplorerViewSettings settings, ColumnDefinition column, Composite rendered) {
        super(rendered, settings.getTreeColumnSettings(), column);
    }

    private boolean isForTestCaseNode(Event event) {
        return event.item.getData() instanceof TestCaseNode;
    }

    private boolean shouldRenderForeground(Event event) {
        return forThisColumn(event)
            && event.item != null
            && isForTestCaseNode(event);
    }
}
