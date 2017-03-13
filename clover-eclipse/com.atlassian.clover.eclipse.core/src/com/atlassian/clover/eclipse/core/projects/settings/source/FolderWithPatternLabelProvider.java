package com.atlassian.clover.eclipse.core.projects.settings.source;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

public class FolderWithPatternLabelProvider extends JavaElementLabelProvider implements ILabelProvider {
    @Override
    public Image getImage(Object element) {
        return super.getImage(((SourceRootWithPattern) element).getPfRoot());
    }

    @Override
    public String getText(Object element) {
        SourceFolderPattern sfp = ((SourceRootWithPattern) element).getPattern();
        return sfp.getSrcPath() + " [includes=" + sfp.getIncludePattern() + "][excludes=" + sfp.getExcludePattern() + "]";
    }
}
