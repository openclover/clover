package org.openclover.idea.actions.testviewscope;

import org.openclover.idea.config.TestViewScope;

public class StatementScopeAction extends AbstractTestViewScopeAction {
    @Override
    protected TestViewScope getActionType() {
        return TestViewScope.STATEMENT;
    }
}
