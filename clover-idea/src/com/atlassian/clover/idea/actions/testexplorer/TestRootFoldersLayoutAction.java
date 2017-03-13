package com.atlassian.clover.idea.actions.testexplorer;

import com.atlassian.clover.idea.config.TestCaseLayout;

public class TestRootFoldersLayoutAction extends AbstractTestLayoutAction {
    @Override
    protected TestCaseLayout getActionType() {
        return TestCaseLayout.SOURCE_ROOTS;
    }
}
