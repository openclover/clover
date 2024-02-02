package org.openclover.idea.treetables;

import org.openclover.idea.util.ComparatorUtil;
import com.intellij.util.ui.ColumnInfo;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;

public abstract class AbstractColumnInfo<T> extends ColumnInfo<DefaultMutableTreeNode, T>
        implements Comparator<DefaultMutableTreeNode> {

    private static final TableCellRenderer DEFAULT_CELL_RENDERER = new DefaultTableCellRenderer();
    protected static final DefaultTableCellRenderer RALIGN_CELL_RENDERER = new DefaultTableCellRenderer();

    static {
        RALIGN_CELL_RENDERER.setHorizontalAlignment(JLabel.RIGHT);
    }

    private final TableCellRenderer cellRenderer;


    public AbstractColumnInfo(String columnName, TableCellRenderer cellRenderer) {
        super(columnName);
        this.cellRenderer = cellRenderer;
    }

    public AbstractColumnInfo(String columnName) {
        this(columnName, DEFAULT_CELL_RENDERER);
    }

    @Override
    public TableCellRenderer getRenderer(DefaultMutableTreeNode defaultMutableTreeNode) {
        return cellRenderer;
    }

    protected int getWidth(JTable jTable, String columnSizer) {
        return DEFAULT_CELL_RENDERER
                .getTableCellRendererComponent(jTable, columnSizer, false, false, 0, 0)
                .getPreferredSize().width;
    }


    /**
     * Return this as comparator only when (overriden) valueOf() returns a Comparable value.
     *
     * @param node1 node1
     * @param node2 node2
     * @return comparison result of respective valueOf() objects
     * @throws ClassCastException when used improperly
     */
    @Override
    public int compare(DefaultMutableTreeNode node1, DefaultMutableTreeNode node2) {
        @SuppressWarnings("unchecked")
        final Comparable<T> left = (Comparable<T>) valueOf(node1);
        final T right = valueOf(node2);

        return ComparatorUtil.compare(left, right);
    }
}
