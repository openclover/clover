package org.openclover.eclipse.core.views;

import org.openclover.core.reporters.Column;
import org.openclover.core.util.MetricsFormatUtils;

public class BuiltinDecimalMetricsColumnDefinition extends BuiltinMetricsColumnDefinition {
    public BuiltinDecimalMetricsColumnDefinition(Column prototype, String abbreviatedTitle, int requiredIndex, int style) {
        super(prototype, abbreviatedTitle, requiredIndex, style);
    }

    @Override
    protected String format(Number value) {
        return MetricsFormatUtils.formatMetricsDecimal(value.doubleValue());
    }
}
