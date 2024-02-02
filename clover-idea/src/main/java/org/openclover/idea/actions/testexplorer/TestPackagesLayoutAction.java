package org.openclover.idea.actions.testexplorer;

import org.openclover.idea.config.TestCaseLayout;

public class TestPackagesLayoutAction extends AbstractTestLayoutAction {
    @Override
    protected TestCaseLayout getActionType() {
        return TestCaseLayout.PACKAGES;
    }
}
