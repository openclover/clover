package com.atlassian.clover.idea.actions.testviewscope;

import com.atlassian.clover.idea.config.TestViewScope;

public class ProjectScopeAction extends AbstractTestViewScopeAction {
    @Override
    protected TestViewScope getActionType() {
        return TestViewScope.GLOBAL;
    }
}
