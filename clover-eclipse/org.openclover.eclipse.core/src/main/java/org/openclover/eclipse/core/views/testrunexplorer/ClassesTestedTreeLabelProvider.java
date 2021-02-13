package com.atlassian.clover.eclipse.core.views.testrunexplorer;

import com.atlassian.clover.eclipse.core.views.ExplorerViewLabelProvider;
import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;

public class ClassesTestedTreeLabelProvider extends ExplorerViewLabelProvider {
    public ClassesTestedTreeLabelProvider(TestRunExplorerViewSettings settings) {
        super(settings, settings.getClassesTestedTreeSettings());
    }

    @Override
    public MetricsScope getMetricsScope() {
        return MetricsScope.APP_ONLY;
    }
}
