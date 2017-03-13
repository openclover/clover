package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.util.ModelScope;

public class ShowTestClassesOnlyAction extends AbstractClassesScopeAction {

    @Override
    protected ModelScope getActionType() {
        return ModelScope.TEST_CLASSES_ONLY;
    }
}
