package com.atlassian.clover.idea.actions.testexplorer;

import com.atlassian.clover.idea.config.TestCaseLayout;

public class TestCasesListLayoutAction extends AbstractTestLayoutAction {
    @Override
    protected TestCaseLayout getActionType() {
        return TestCaseLayout.TEST_CASES;
    }
}
