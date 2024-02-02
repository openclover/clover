package org.openclover.idea.actions.testexplorer;

import org.openclover.idea.config.TestCaseLayout;

public class TestCasesListLayoutAction extends AbstractTestLayoutAction {
    @Override
    protected TestCaseLayout getActionType() {
        return TestCaseLayout.TEST_CASES;
    }
}
