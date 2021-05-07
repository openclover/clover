package org.openclover.eclipse.core.projects.settings.source;

public interface InstrumentedSourceFoldersListener {
    void selectionChanged(SourceRootWithPattern pattern);
    void perFolderSelectionEnabled(boolean enabled);
}
