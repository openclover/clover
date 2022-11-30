package com.atlassian.clover.idea;

import com.intellij.ide.SelectInContext;

public interface SelectInCloverView {
    /**
     * @param context context of select request
     * @return true if the view is able to select specified context
     */
    boolean canSelect(SelectInContext context);

    /**
     * @param context context of select request
     * @return true if the view has consumed the request
     */
    boolean selectIn(SelectInContext context);
}
