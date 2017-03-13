package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.idea.treetables.AbstractColumnInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
import java.awt.Component;
import java.util.Comparator;

public class TestStatusColumnInfo extends AbstractColumnInfo<DefaultMutableTreeNode> {
    public TestStatusColumnInfo() {
        super("Status", STATUS_CELL_RENDERER);
    }

    @Override
    public DefaultMutableTreeNode valueOf(DefaultMutableTreeNode defaultMutableTreeNode) {
        return defaultMutableTreeNode;
    }

    /**
     * Set fixed column size.
     */
    @Override
    public int getWidth(JTable jTable) {
        return getWidth(jTable, " ERROR ");
    }

    private static final TableCellRenderer STATUS_CELL_RENDERER = new DefaultTableCellRenderer() {
        {
            setHorizontalAlignment(CENTER);
        }

        //hack necessary because sth was overriding the setOpaque/setBackground just before painting
        private Color colorOverride;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof TestCaseInfo) {
                Status status = Status.resolve((TestCaseInfo) userObject);
                setText(status.toString());
                colorOverride = status.getColor();
            } else {
                setText("");
                colorOverride = null;
            }
            return this;
        }

        @Override
        public boolean isOpaque() {
            return colorOverride != null || super.isOpaque();
        }

        @Override
        public Color getBackground() {
            return colorOverride != null ? colorOverride : super.getBackground();
        }
    };

    private enum Status {
        NA(null) {
            @Override
            public String toString() {
                return "";
            }},
        PASS(Color.GREEN),
        FAIL(Color.RED),
        ERROR(Color.ORANGE);

        private final Color color;

        Status(Color color) {
            this.color = color;
        }

        Color getColor() {
            return color;
        }

        static Status resolve(TestCaseInfo tci) {
            if (tci == null) {
                return NA;
            } else if (tci.isSuccess()) {
                return PASS;
            } else if (tci.isError()) {
                return ERROR;
            } else if (tci.isFailure()) {
                return FAIL;
            } else {
                return NA;
            }
        }
    }

    @Override
    public Comparator<DefaultMutableTreeNode> getComparator() {
        return COMPARATOR;
    }

    private static final Comparator<DefaultMutableTreeNode> COMPARATOR = new AbstractTestCaseNodeComparator() {
        @Override
        int compare(TestCaseInfo tci1, TestCaseInfo tci2) {
            final Status status1 = Status.resolve(tci1);
            final Status status2 = Status.resolve(tci2);

            return status1.compareTo(status2);
        }
    };
}
