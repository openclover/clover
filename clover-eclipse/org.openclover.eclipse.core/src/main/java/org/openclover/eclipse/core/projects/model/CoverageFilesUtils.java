package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.io.FileFilter;

public class CoverageFilesUtils {
    public static void deleteCoverageFiles(final File coverageDbFile, final boolean deleteDbToo, IProgressMonitor monitor) {
        final File coverageDbFolder = coverageDbFile.getParentFile();

        if (coverageDbFolder.exists()) {
            monitor.subTask("Removing Clover recording files");
            FileFilter deleteFilter = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.toString().indexOf(coverageDbFile.getPath()) == 0) {
                        return
                            (deleteDbToo && (pathname.toString().length() == coverageDbFile.getPath().length()))
                            || (pathname.toString().length() != coverageDbFile.getPath().length());
                    }
                    return false;
                }
            };
            File[] filesToDelete = coverageDbFolder.listFiles(deleteFilter);
            for (File toDelete : filesToDelete) {
                toDelete.delete();
            }
        }
    }
}
