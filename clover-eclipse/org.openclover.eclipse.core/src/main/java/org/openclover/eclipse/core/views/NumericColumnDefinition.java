package org.openclover.eclipse.core.views;

import org.openclover.eclipse.core.projects.model.MetricsScope;

public interface NumericColumnDefinition {
    Double NOT_AVAILABLE_DOUBLE = -1.0d;

    Number getValue(MetricsScope scope, Object object);
}
