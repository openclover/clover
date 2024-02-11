package org.openclover.eclipse.core.ui.editors.treemap;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.metrics.BlockMetrics;
import net.sf.jtreemap.ktreemap.ITreeMapProvider;
import net.sf.jtreemap.ktreemap.TreeMapNode;

import java.text.DecimalFormat;

public class CloverTreeMapProvider implements ITreeMapProvider {

    private final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("###.##%");

    @Override
    public String getLabel(TreeMapNode node) {
        if (node.getValue() instanceof HasMetrics) {
            return ((HasMetrics)node.getValue()).getName();
        } else {
            return "";
        }
    }

    @Override
    public String getValueLabel(Object value) {
         //Format objects not thread-safe
         synchronized (PERCENTAGE_FORMAT) {
             return PERCENTAGE_FORMAT.format(getDoubleValue(value));
         }
     }

    @Override
    public double getDoubleValue(Object value) {
        if (value instanceof HasMetrics) {
            final float pcCoveredElements = ((HasMetrics) value).getMetrics().getPcCoveredElements();
            return pcCoveredElements == BlockMetrics.VALUE_UNDEFINED ? BlockMetrics.VALUE_UNDEFINED : pcCoveredElements;
        } else {
            return 0.0d;
        }
    }
}
