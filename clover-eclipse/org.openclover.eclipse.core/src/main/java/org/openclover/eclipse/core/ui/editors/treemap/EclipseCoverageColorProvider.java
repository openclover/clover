package org.openclover.eclipse.core.ui.editors.treemap;

import com.atlassian.clover.reporters.util.ReportColors;
import net.sf.jtreemap.ktreemap.ITreeMapColorProvider;
import net.sf.jtreemap.ktreemap.ITreeMapProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class EclipseCoverageColorProvider implements ITreeMapColorProvider {

    private final ITreeMapProvider iTreeMapProvider;

    public EclipseCoverageColorProvider(ITreeMapProvider iTreeMapProvider) {
        this.iTreeMapProvider = iTreeMapProvider;
    }

    @Override
    public Color getForeground(Object o) {
        return Display.getDefault().getSystemColor(1);
    }

    @Override
    public Color getBackground(Object o) {
        final double doubleValue = iTreeMapProvider.getDoubleValue(o);
        return new Color(Display.getDefault(),
                ReportColors.ADG_COLORS.getColor(doubleValue).getRed(),
                ReportColors.ADG_COLORS.getColor(doubleValue).getGreen(),
                ReportColors.ADG_COLORS.getColor(doubleValue).getBlue());
    }

    @Override
    public Composite getLegend(Composite composite, int i) {
        return null;
    }
}
