package org.openclover.idea.actions.testviewscope;

import org.openclover.idea.config.TestViewScope;

public class ClassScopeAction extends AbstractTestViewScopeAction {
    @Override
    protected TestViewScope getActionType() {
        return TestViewScope.CLASS;
    }
}
