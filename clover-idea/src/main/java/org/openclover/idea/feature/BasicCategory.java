package org.openclover.idea.feature;

public class BasicCategory extends AbstractCategory {

    private boolean enabled;

    public BasicCategory(String categoryName) {
        super(categoryName);
    }

    @Override
    public void setEnabled(boolean b) {
        if (enabled == b) {
            return;
        }
        enabled = b;
        CategoryEvent evt = new CategoryEvent(this, getName(), enabled);
        fireCategoryEvent(evt);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
