package org.openclover.idea.actions;

import com.atlassian.clover.api.registry.HasMetrics;
import com.intellij.openapi.actionSystem.DataKey;

public final class Constants {
    private Constants() {
    }

    private static final String CONSTANTS_PREFIX = Constants.class.getName() + "-";
    public static final DataKey<HasMetrics> SELECTED_HAS_METRICS = DataKey.create(CONSTANTS_PREFIX + "selected-has-metrics");

}
