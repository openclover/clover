package org.openclover.idea.treetables;

import org.openclover.idea.util.ui.ScrollToSourceMouseAdapter;
import org.openclover.idea.util.ui.TreeExpansionHelper;
import org.openclover.idea.util.ui.TreeSelectionHelper;
import org.openclover.idea.util.ui.TreeUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.dualView.TreeTableView;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

public class TreeTablePanel extends JPanel {

    protected final TreeTableView treeTableView;
    protected final DefaultMutableTreeNode rootNode;
    protected final SortableListTreeTableModelOnColumns tableModel;

    protected boolean alwaysExpandTestCases;
    protected boolean alwaysCollapseTestCases;

    public TreeTablePanel(Project project, SortableListTreeTableModelOnColumns model) {
        setLayout(new BorderLayout());
        rootNode = new DefaultMutableTreeNode();
        tableModel = model;
        tableModel.setRoot(rootNode);

        treeTableView = new TreeTableView(tableModel);


        treeTableView.setRootVisible(false);

        treeTableView.setTreeCellRenderer(new ProjectTreeCellRenderer());
        treeTableView.getTree().setShowsRootHandles(true);

        treeTableView.getTableHeader().setDefaultRenderer(new JTableHeader().getDefaultRenderer()); // workaround for no drag handles on MacOSX
        treeTableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTableView.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        treeTableView.setAutoscrolls(true);

        treeTableView.addMouseListener(ScrollToSourceMouseAdapter.getInstance(project));

        treeTableView.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final TableColumnModel columnModel = treeTableView.getTableHeader().getColumnModel();
                final int column = columnModel.getColumnIndexAtX(e.getX());
                final int modelColumn = columnModel.getColumn(column).getModelIndex();

                tableModel.sortByColumn(modelColumn);
                sortNodes();
            }
        });

    }


    public void clean() {
        rootNode.removeAllChildren();
        tableModel.nodeStructureChanged(rootNode);
    }

    protected void expandAll(boolean expand) {
        final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tableModel.getRoot();
        Enumeration nodes = rootNode.postorderEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) nodes.nextElement();
            if (child != rootNode && !child.isLeaf()) {
                TreePath path = new TreePath(child.getPath());
                if (expand) {
                    treeTableView.getTree().expandPath(path);
                } else {
                    treeTableView.getTree().collapsePath(path);
                }
            }
        }
    }

    protected void sortNodes() {
        final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tableModel.getRoot();
        final boolean customExpand = !alwaysExpandTestCases && !alwaysCollapseTestCases;
        TreeSelectionHelper teh = customExpand ?
                new TreeExpansionHelper(treeTableView.getTree()) :
                new TreeSelectionHelper(treeTableView.getTree());
        TreeUtil.sortNodes(rootNode, tableModel);
        tableModel.reload();
        if (!customExpand && alwaysExpandTestCases) {
            expandAll(true);
        }
        teh.restore(treeTableView.getTree());
    }
}
