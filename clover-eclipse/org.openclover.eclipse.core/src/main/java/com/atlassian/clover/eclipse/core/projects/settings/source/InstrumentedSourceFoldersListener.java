package com.atlassian.clover.eclipse.core.projects.settings.source;

public interface InstrumentedSourceFoldersListener {
    void selectionChanged(SourceRootWithPattern pattern);
    void perFolderSelectionEnabled(boolean enabled);
}
