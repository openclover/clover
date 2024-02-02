package org.openclover.idea.actions;

import org.openclover.idea.util.ModelScope;

public class ShowApplicationClassesOnlyAction extends AbstractClassesScopeAction {
    @Override
    protected ModelScope getActionType() {
        return ModelScope.APP_CLASSES_ONLY;
    }
}
