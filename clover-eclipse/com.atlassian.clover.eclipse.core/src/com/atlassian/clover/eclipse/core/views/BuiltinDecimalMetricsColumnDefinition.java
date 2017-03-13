package com.atlassian.clover.eclipse.core.views;

import com.atlassian.clover.util.MetricsFormatUtils;
import com.atlassian.clover.reporters.Column;

public class BuiltinDecimalMetricsColumnDefinition extends BuiltinMetricsColumnDefinition {
    public BuiltinDecimalMetricsColumnDefinition(Column prototype, String abbreviatedTitle, int requiredIndex, int style) {
        super(prototype, abbreviatedTitle, requiredIndex, style);
    }

    @Override
    protected String format(Number value) {
        return MetricsFormatUtils.formatMetricsDecimal(value.doubleValue());
    }
}
