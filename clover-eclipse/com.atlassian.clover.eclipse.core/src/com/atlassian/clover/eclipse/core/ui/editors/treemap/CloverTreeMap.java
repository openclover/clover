package com.atlassian.clover.eclipse.core.ui.editors.treemap;

import com.atlassian.clover.api.registry.PackageInfo;
import net.sf.jtreemap.ktreemap.KTreeMap;
import net.sf.jtreemap.ktreemap.TreeMapNode;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;

import java.util.List;

import com.atlassian.clover.eclipse.core.views.actions.OpenJavaEditorAction;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.api.registry.HasMetrics;

public class CloverTreeMap extends KTreeMap {
    private OpenJavaEditorAction openEditorAction;

    public CloverTreeMap(final IJavaProject project, IWorkbenchPartSite site, Composite parent, int root, TreeMapNode strategy) {
        super(parent, root, strategy);

        openEditorAction = new OpenJavaEditorAction(site);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent mouseEvent) {
                if (getActiveLeaf().isLeaf()) {
                    HasMetrics activeMetricsLeaf = ((HasMetrics) getActiveLeaf().getValue());
                    HasMetrics activeMetricsBranch = ((HasMetrics) getActiveLeaf().getParent().getValue());
                    try {
                        IType type =
                            project == null
                                ? null
                                : project.findType(
                                    getPackageName(activeMetricsBranch) + activeMetricsLeaf.getName(),
                                    (IProgressMonitor)null);

                        if (type != null) {
                            openEditorAction.run(new StructuredSelection(type));
                        } else {
                            CloverPlugin.logWarning("Unable to open editor for treemap leaf: " + activeMetricsLeaf.getName());
                        }
                    } catch (CoreException e) {
                        CloverPlugin.logError("Unable to open editor for treemap leaf: " + activeMetricsLeaf.getName(), e);
                    }

                }
            }

            private String getPackageName(HasMetrics hasMetrics) {
                return
                    PackageInfo.DEFAULT_PACKAGE_NAME.equals(hasMetrics.getName())
                    ? ""
                    : (hasMetrics.getName() + ".");
            }
        });

    }


    @Override
    protected void drawLabels(GC gc, TreeMapNode item) {
        gc.setFont(this.getFont());
        if (!getDisplayedRoot().isLeaf()) {
            List<TreeMapNode> children = getDisplayedRoot().getChildren();
            for (TreeMapNode child : children) {
                drawLabel(gc, child);
            }
        }
    }

    @Override
    protected void drawLabel(GC gc, TreeMapNode item) {
        if (item.isLeaf()) {
            //Eliding class names is better done from the rear
            super.drawLabel(gc, item);
        } else {
            //Eliding package names is better done from the front
            FontMetrics fm = gc.getFontMetrics();
            // if the height of the item is high enough
            if (fm.getHeight() < item.getHeight() - 2) {
                String label = getTreeMapProvider().getLabel(item);

                int y = (item.getHeight() - fm.getAscent() - fm.getLeading() + fm
                    .getDescent()) / 2;
                int stringWidth = fm.getAverageCharWidth() * label.length();
                // the width of the label depends on the font :
                // if the width of the label is larger than the item
                if (item.getWidth() - 5 <= stringWidth) {
                    // We have to truncate the label
                    // number of chars who can be writen in the item
                    int nbChar = (label.length() * item.getWidth()) / stringWidth;
                    if (nbChar > 3) {
                        // and add "..." at the end
                        label = "..." + label.substring(Math.max(0, (label.length() - 1) - (nbChar)), label.length());
                        stringWidth = (nbChar - 1) * fm.getAverageCharWidth();
                    } else {
                        // if it is not enough large, we display nothing
                        return;
                    }
                }
                int x = (item.getWidth() - stringWidth) / 2;

                // background in black
                gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                gc.drawString(label, item.getX() + x + 1, item.getY() + y + 1, true);
                gc.drawString(label, item.getX() + x - 1, item.getY() + y + 1, true);
                gc.drawString(label, item.getX() + x + 1, item.getY() + y - 1, true);
                gc.drawString(label, item.getX() + x - 1, item.getY() + y - 1, true);
                gc.drawString(label, item.getX() + x + 1, item.getY() + y, true);
                gc.drawString(label, item.getX() + x - 1, item.getY() + y, true);
                gc.drawString(label, item.getX() + x, item.getY() + y + 1, true);
                gc.drawString(label, item.getX() + x, item.getY() + y - 1, true);
                // label in foreground color
                gc.setForeground(getColorProvider().getForeground(item));
                gc.drawString(label, item.getX() + x, item.getY() + y, true);
            }
        }
    }
}
