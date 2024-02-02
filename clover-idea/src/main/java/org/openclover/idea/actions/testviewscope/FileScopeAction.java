package com.atlassian.clover.idea.actions.testviewscope;

import com.atlassian.clover.idea.config.TestViewScope;

public class FileScopeAction extends AbstractTestViewScopeAction {
    @Override
    protected TestViewScope getActionType() {
        return TestViewScope.FILE;
    }
}
