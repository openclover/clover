package org.openclover.eclipse.core.projects.settings.source;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Set of utility functions for managing layout.
 */
public class LayoutUtils {

    public static Group createGroup(Composite parent, String title) {
        Group group = new Group(parent, SWT.SHADOW_NONE);
        if (title != null) {
            group.setText(title);
        }
        return group;
    }
}
