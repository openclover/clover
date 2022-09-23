package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.ant.AntFileSetUtils;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.Project;

import java.io.File;
import java.util.List;

import com.atlassian.clover.Logger;

public interface FilesetFileVisitor {

    void visit(File file);

    class Util {

        static void collectFiles(Project project, List<FileSet> fileSets, FilesetFileVisitor visitor) {
            collectFiles(project, fileSets, false, visitor);
        }

        /**
         * Visits all files in the given list of filesets.
         * @param project the project owning the filesets
         * @param fileSets the filesets to visit
         * @param continueIfMissing whether or not to continue if a directory is missing or is a file
         * @param visitor the visitor to call
         */
        static void collectFiles(Project project, List<FileSet> fileSets,  boolean continueIfMissing, FilesetFileVisitor visitor) {
            for (final FileSet fileset : fileSets) {
                // warn if base directory is missing
                final File baseDir = fileset.getDir(project);
                if (continueIfMissing && (!baseDir.exists() || !baseDir.isDirectory())) {
                    Logger.getInstance().debug("Invalid directory specified: " + baseDir.getAbsolutePath() + ". Ignoring");
                    continue;
                }

                // warn if inclusion/exclusion patterns contain leading/trailing whitespace
                AntFileSetUtils.checkForNonTrimmedPatterns(fileset, project);

                final String[] includedFiles = fileset.getDirectoryScanner(project).getIncludedFiles();
                for (final String includedFile : includedFiles) {
                    visitor.visit(new File(baseDir, includedFile));
                }
            }
        }

    }

}



