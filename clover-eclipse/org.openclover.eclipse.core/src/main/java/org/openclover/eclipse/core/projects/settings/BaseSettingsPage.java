package org.openclover.eclipse.core.projects.settings;

import org.eclipse.ui.dialogs.PropertyPage;
import org.openclover.eclipse.core.ui.CharDimensionConverter;

public abstract class BaseSettingsPage extends PropertyPage implements CharDimensionConverter {

    @Override
    public int convertHeightInCharsToPixels(int i) {
        return super.convertHeightInCharsToPixels(i);
    }

    @Override
    public int convertWidthInCharsToPixels(int i) {
        return super.convertWidthInCharsToPixels(i);
    }
}
