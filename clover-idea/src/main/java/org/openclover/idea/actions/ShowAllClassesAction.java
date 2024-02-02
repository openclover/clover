package org.openclover.idea.actions;

import org.openclover.idea.util.ModelScope;

public class ShowAllClassesAction extends AbstractClassesScopeAction {

    @Override
    protected ModelScope getActionType() {
        return ModelScope.ALL_CLASSES;
    }
}
