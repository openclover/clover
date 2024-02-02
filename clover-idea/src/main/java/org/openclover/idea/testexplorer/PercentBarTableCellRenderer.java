package org.openclover.idea.testexplorer;

import org.openclover.idea.coverage.PercentBarColors;
import org.openclover.idea.coverage.PercentBarPanel;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.EnumMap;
import java.util.Map;

public class PercentBarTableCellRenderer extends PercentBarPanel implements TableCellRenderer {
    private static final DefaultTableCellRenderer EMPTY_RENDERER = new DefaultTableCellRenderer();

    public PercentBarTableCellRenderer(PercentBarColors colors) {
        super(colors);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value == null) {
            return EMPTY_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            final Float pc = (Float) value;
            setPercent(pc);
            return this;
        }
    }

    private static Map<PercentBarColors, PercentBarTableCellRenderer> INSTANCES =
            new EnumMap<>(PercentBarColors.class);

    public static TableCellRenderer getInstance() {
        return getInstance(PercentBarColors.LIGHTBLUE_ON_WHITE);
    }

    public static TableCellRenderer getInstance(PercentBarColors colors) {
        PercentBarTableCellRenderer instance = INSTANCES.get(colors);
        if (instance == null) {
            instance = new PercentBarTableCellRenderer(colors);
            INSTANCES.put(colors, instance);
        }
        return instance;
    }

}
