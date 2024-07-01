package org.openclover.idea.report.treemap;

import net.sf.jtreemap.swing.JTreeMap;
import net.sf.jtreemap.swing.TreeMapNode;
import net.sf.jtreemap.swing.Value;
import com.intellij.openapi.project.Project;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.idea.util.ui.ScrollUtil;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GoToSourceMouseListener extends MouseAdapter {
    private final JTreeMap treeMap;
    private final Project project;

    public GoToSourceMouseListener(Project project, JTreeMap treeMap) {
        this.project = project;
        this.treeMap = treeMap;
    }


    private ClassInfoValue getActiveClassInfoValue() {
        final TreeMapNode node = treeMap.getActiveLeaf();
        if (node != null) {
            final Value value = node.getValue();
            if (value instanceof ClassInfoValue) {
                return (ClassInfoValue) value;
            }
        }
        return null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        processEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        processEvent(e);
    }

    private void processEvent(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        final ClassInfoValue classInfoValue = getActiveClassInfoValue();

        if (classInfoValue != null) {
            JPopupMenu menu = new JPopupMenu();

            final ClassInfo classInfo = classInfoValue.getClassInfo();
            final BlockMetrics classMetrics = classInfo.getMetrics();

            final String info = "covered " + classMetrics.getNumCoveredElements() + " / "
                    + classMetrics.getNumElements() + " elements";
            final JLabel label = new JLabel("<html><b><center>" + classInfo.getName() + "</center></b><small>" + info);
            menu.add(label);
            menu.addSeparator();

            JMenuItem menuItem = menu.add("Jump to source");
            menuItem.addActionListener(e1 -> ScrollUtil.scrollToSourceRegion(project, classInfo));

            menu.show(e.getComponent(), e.getX(), e.getY());
        }

    }

}
