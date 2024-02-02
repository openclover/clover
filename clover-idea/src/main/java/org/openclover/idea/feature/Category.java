package org.openclover.idea.feature;

/**
 *
 *
 */
public interface Category {

    void setEnabled(boolean b);

    boolean isEnabled();

    String getName();

    void addCategoryListener(CategoryListener l);

    void removeCategoryListener(CategoryListener l);
}
