package com.atlassian.clover.idea.report.jfc;

import com.atlassian.clover.idea.coverage.PercentBarRenderer;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;

/**
 *
 */
public class PercentBarIcon implements Icon {

    // between 0..1 or -1 if NA
    private float percent;

    private PercentBarRenderer renderer = new PercentBarRenderer();

    private int iconWidth;
    private int iconHeight;

    public PercentBarIcon(int iconWidth, int iconHeight) {
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
    }

    @Override
    public int getIconWidth() {
        return iconWidth;
    }

    public void setIconWidth(int iconWidth) {
        this.iconWidth = iconWidth;
    }

    @Override
    public int getIconHeight() {
        return iconHeight;
    }

    public void setIconHeight(int iconHeight) {
        this.iconHeight = iconHeight;
    }

    public void setPercent(float pc) {
        percent = pc;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        renderer.renderBar(c, g, percent, false, x, y, iconWidth, iconHeight);
    }


}
