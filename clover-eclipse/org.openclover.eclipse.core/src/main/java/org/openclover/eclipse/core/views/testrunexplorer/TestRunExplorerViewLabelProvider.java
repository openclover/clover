package org.openclover.eclipse.core.views.testrunexplorer;

import org.openclover.eclipse.core.views.ExplorerViewLabelProvider;

public class TestRunExplorerViewLabelProvider extends ExplorerViewLabelProvider {
    public TestRunExplorerViewLabelProvider(final TestRunExplorerViewSettings settings) {
        super(settings, settings.getTreeColumnSettings());
    }
}
