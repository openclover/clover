package com.atlassian.clover.eclipse.core.views;

import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.util.MetricsFormatUtils;

public class BuiltinIntegralMetricsColumnDefinition extends BuiltinMetricsColumnDefinition {
    public BuiltinIntegralMetricsColumnDefinition(Column prototype, String abbreviatedTitle, int requiredIndex, int style) {
        super(prototype, abbreviatedTitle, requiredIndex, style);
    }

    @Override
    protected String format(Number value) {
        return MetricsFormatUtils.formatMetricsInteger(value.longValue());
    }
}
