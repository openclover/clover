package com.atlassian.clover.idea.actions.testexplorer;

import com.atlassian.clover.idea.config.TestCaseLayout;

public class TestPackagesLayoutAction extends AbstractTestLayoutAction {
    @Override
    protected TestCaseLayout getActionType() {
        return TestCaseLayout.PACKAGES;
    }
}
