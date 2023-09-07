package org.openclover.eclipse.core.views.testrunexplorer;

import org.openclover.eclipse.core.views.ExplorerViewLabelProvider;
import org.openclover.eclipse.core.projects.model.MetricsScope;

public class ClassesTestedTreeLabelProvider extends ExplorerViewLabelProvider {
    public ClassesTestedTreeLabelProvider(TestRunExplorerViewSettings settings) {
        super(settings, settings.getClassesTestedTreeSettings());
    }

    @Override
    public MetricsScope getMetricsScope() {
        return MetricsScope.APP_ONLY;
    }
}
