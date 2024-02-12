package org.openclover.eclipse.core.views.testrunexplorer;

import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.ExplorerViewLabelProvider;

public class ClassesTestedTreeLabelProvider extends ExplorerViewLabelProvider {
    public ClassesTestedTreeLabelProvider(TestRunExplorerViewSettings settings) {
        super(settings, settings.getClassesTestedTreeSettings());
    }

    @Override
    public MetricsScope getMetricsScope() {
        return MetricsScope.APP_ONLY;
    }
}
