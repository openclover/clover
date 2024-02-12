package org.openclover.idea.coverage;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;

/**
 *
 */
public class PercentBarPanel extends JPanel
        implements TableCellRenderer {


    // between 0..1 or -1 if NA
    private float percent;

    private final PercentBarRenderer renderer;

    public PercentBarPanel() {
        this.renderer = new PercentBarRenderer();
    }

    public PercentBarPanel(PercentBarColors colors) {
        this.renderer = new PercentBarRenderer(colors);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int height = getHeight();
        int width = getWidth();
        renderer.renderBar(this, g, percent, width, height);
    }


    public void setPercent(float pc) {
        percent = pc;
        repaint();
    }


    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        try {
            percent = (Float) value;
        } catch (ClassCastException e) {
            percent = -1;
        }

        return this;
    }


}
