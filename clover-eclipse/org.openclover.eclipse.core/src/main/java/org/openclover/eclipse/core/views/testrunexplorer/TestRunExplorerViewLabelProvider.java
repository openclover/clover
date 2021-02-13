package com.atlassian.clover.eclipse.core.views.testrunexplorer;

import com.atlassian.clover.eclipse.core.views.ExplorerViewLabelProvider;

public class TestRunExplorerViewLabelProvider extends ExplorerViewLabelProvider {
    public TestRunExplorerViewLabelProvider(final TestRunExplorerViewSettings settings) {
        super(settings, settings.getTreeColumnSettings());
    }
}
