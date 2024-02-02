package org.openclover.idea.actions.testviewscope;

import org.openclover.idea.config.TestViewScope;

public class MethodScopeAction extends AbstractTestViewScopeAction {
    @Override
    protected TestViewScope getActionType() {
        return TestViewScope.METHOD;
    }
}
