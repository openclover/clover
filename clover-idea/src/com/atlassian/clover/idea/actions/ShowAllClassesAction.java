package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.util.ModelScope;

public class ShowAllClassesAction extends AbstractClassesScopeAction {

    @Override
    protected ModelScope getActionType() {
        return ModelScope.ALL_CLASSES;
    }
}
