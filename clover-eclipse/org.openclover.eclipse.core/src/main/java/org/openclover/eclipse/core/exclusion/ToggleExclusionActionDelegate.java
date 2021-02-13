package com.atlassian.clover.eclipse.core.exclusion;

public class ToggleExclusionActionDelegate extends BaseToggleActionDelegate {

    @Override
    protected String getActionName() {
        return "Exclude";
    }

    @Override
    protected boolean isExclude() {
        return true;
    }

}
