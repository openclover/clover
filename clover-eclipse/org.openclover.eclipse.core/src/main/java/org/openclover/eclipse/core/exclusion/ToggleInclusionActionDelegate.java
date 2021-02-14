package org.openclover.eclipse.core.exclusion;

public class ToggleInclusionActionDelegate extends BaseToggleActionDelegate {

    @Override
    protected String getActionName() {
        return "Include";
    }

    @Override
    protected boolean isExclude() {
        return false;
    }

}
