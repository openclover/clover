package org.openclover.idea.coverage;

import org.openclover.idea.coverage.PercentBarColors;
import com.atlassian.clover.util.Formatting;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 *
 */
public class PercentBarRenderer {

    private final Color foreground;
    private final Color background;

    /**
     * Green-Red bar by default.
     */
    public PercentBarRenderer() {
        this(PercentBarColors.GREEN_ON_RED);
    }

    public PercentBarRenderer(PercentBarColors colors) {
        this.foreground = colors.getForeground();
        this.background = colors.getBackground();
    }


    public void renderBar(Component c, Graphics g, float percent, int width, int height) {
        renderBar(c, g, percent, true, 0, 0, width, height);
    }

    public void renderBar(Component c, Graphics g, float percent, boolean showText, int originx, int originy, int width, int height) {
        Color oldColor = g.getColor();

        boolean drawpc = (percent >= 0);
        int maxTextWidth = 0;

        if (showText) {
            FontMetrics metrics = c.getFontMetrics(c.getFont());
            maxTextWidth = metrics.stringWidth(" 999.9% ");
            final int textHeight = metrics.getAscent();
            String pc = " " + Formatting.getPercentStr(percent) + " ";
            final int textWidth = metrics.stringWidth(pc);
            g.setColor(Color.black);
            g.drawString(pc, originx + (maxTextWidth - textWidth), originy + ((height + textHeight) / 2 - 1));
        }

        int barWidth = width - maxTextWidth - 4;
        height /= 4;
        if (drawpc) {
            int greenW = (int) (barWidth * percent);
            g.setColor(foreground);
            g.fillRect(originx + maxTextWidth, originy + height, greenW, 2 + height * 2);
            g.setColor(background);
            g.fillRect(originx + maxTextWidth + greenW, originy + height, barWidth - greenW, 2 + height * 2);
        } else {
            g.setColor(new Color(230, 230, 230));
            g.fillRect(originx + maxTextWidth, originy + height, barWidth, 2 + height * 2);
        }
        g.setColor(new Color(120, 120, 120));
        g.drawRect(originx + maxTextWidth, originy + height, barWidth, 2 + height * 2);

        g.setColor(oldColor);
    }
}
