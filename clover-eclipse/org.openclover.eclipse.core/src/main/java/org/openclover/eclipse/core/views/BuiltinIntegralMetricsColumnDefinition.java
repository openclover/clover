package org.openclover.eclipse.core.views;

import org.openclover.core.reporters.Column;
import org.openclover.core.util.MetricsFormatUtils;

public class BuiltinIntegralMetricsColumnDefinition extends BuiltinMetricsColumnDefinition {
    public BuiltinIntegralMetricsColumnDefinition(Column prototype, String abbreviatedTitle, int requiredIndex, int style) {
        super(prototype, abbreviatedTitle, requiredIndex, style);
    }

    @Override
    protected String format(Number value) {
        return MetricsFormatUtils.formatMetricsInteger(value.longValue());
    }
}
