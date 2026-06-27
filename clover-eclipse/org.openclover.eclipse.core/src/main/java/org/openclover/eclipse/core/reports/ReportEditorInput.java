package org.openclover.eclipse.core.reports;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.jdt.internal.debug.ui.StorageEditorInput;
import org.eclipse.ui.IPathEditorInput;

import java.io.File;

class ReportEditorInput extends StorageEditorInput implements IPathEditorInput {

    private final File file;

    ReportEditorInput(File file) {
        super(new LocalFileStorage(file));
        this.file = file;
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public IPath getPath() {
        return new Path(file.getPath());
    }
}
