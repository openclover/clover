package org.openclover.idea.actions.testexplorer;

import org.openclover.idea.config.TestCaseLayout;

public class TestRootFoldersLayoutAction extends AbstractTestLayoutAction {
    @Override
    protected TestCaseLayout getActionType() {
        return TestCaseLayout.SOURCE_ROOTS;
    }
}
