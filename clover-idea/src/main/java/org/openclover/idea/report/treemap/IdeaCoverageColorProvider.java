package org.openclover.idea.report.treemap;

import net.sf.jtreemap.swing.provider.ColorProvider;
import net.sf.jtreemap.swing.Value;
import org.openclover.core.reporters.util.ReportColors;

import javax.swing.JPanel;
import java.awt.Color;

public class IdeaCoverageColorProvider extends ColorProvider {

    @Override
    public Color getColor(Value val) {
        return ReportColors.ADG_COLORS.getColor(normalizeValue(val));
    }

    private double normalizeValue(Value val) {
        double value = ReportColors.MIN_VALUE;

        if (val != null) {
            value = val.getValue() > ReportColors.MIN_VALUE ? val.getValue() : ReportColors.MIN_VALUE;
        }
        return value / 100.0;
    }

    @Override
    public JPanel getLegendPanel() {
        return null;
    }
}
