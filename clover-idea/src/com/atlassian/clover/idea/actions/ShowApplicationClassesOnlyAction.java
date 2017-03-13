package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.util.ModelScope;

public class ShowApplicationClassesOnlyAction extends AbstractClassesScopeAction {
    @Override
    protected ModelScope getActionType() {
        return ModelScope.APP_CLASSES_ONLY;
    }
}
