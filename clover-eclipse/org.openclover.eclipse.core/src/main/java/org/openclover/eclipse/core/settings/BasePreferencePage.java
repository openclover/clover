package org.openclover.eclipse.core.settings;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openclover.eclipse.core.ui.CharDimensionConverter;

public abstract class BasePreferencePage
    extends org.eclipse.jface.preference.PreferencePage
    implements IWorkbenchPreferencePage, CharDimensionConverter {

    protected IWorkbench workbench;

    @Override
    public void init(IWorkbench workbench) {
        this.workbench = workbench;
    }

    @Override
    public int convertHeightInCharsToPixels(int chars) {
        return super.convertHeightInCharsToPixels(chars);
    }

    @Override
    public int convertWidthInCharsToPixels(int chars) {
        return super.convertWidthInCharsToPixels(chars);
    }

}
