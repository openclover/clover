package com.atlassian.clover.eclipse.core.views;

import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;

public interface NumericColumnDefinition {
    public static final Double NOT_AVAILABLE_DOUBLE = new Double(-1.0d);

    Number getValue(MetricsScope scope, Object object);
}
