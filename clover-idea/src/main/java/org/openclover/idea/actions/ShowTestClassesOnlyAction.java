package org.openclover.idea.actions;

import org.openclover.idea.util.ModelScope;

public class ShowTestClassesOnlyAction extends AbstractClassesScopeAction {

    @Override
    protected ModelScope getActionType() {
        return ModelScope.TEST_CLASSES_ONLY;
    }
}
