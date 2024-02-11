package org.openclover.core.util;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.Set;
import java.util.Collections;

import static org.openclover.core.util.Sets.newHashSet;

/**
 * Scans a directory for files (a simplified DirectoryScanner)
 */
public class SourceScanner {

    public interface Visitor {
        void common(String path) throws IOException;
        void onlyInSrc(String path) throws IOException;
        void onlyInDest(String path) throws IOException;
    }


    private final File srcDir;
    private final File destDir;
    private final FilenameFilter nameSpecFilter;

    public SourceScanner(File srcDir, final String nameSpec) {
       this(srcDir, null, nameSpec);
    }
    public SourceScanner(File srcDir, File destDir, final String nameSpec) {
        this.srcDir = srcDir;
        this.destDir = destDir;
        this.nameSpecFilter = (parent, name) -> {
            File file = new File(parent, name);
            return file.isDirectory() || name.matches(nameSpec);
        };
    }

    public void visit(Visitor visitor) throws IOException {
        scan(null, visitor);
    }

    private void scan(String path, Visitor visitor) throws IOException {
        File src;
        File dest;

        if (path == null) {
            src = srcDir;
            dest = destDir;
        } else {
            src = new File(srcDir, path);
            dest = destDir != null ? new File(destDir, path) : null;
        }

        if (!src.isDirectory()) {
            return;
        }

        final String[] srcFiles = src.list(nameSpecFilter);
        final String[] destFiles = dest != null ? dest.list(nameSpecFilter) : null;
        final Set<String> destFileSet = destFiles != null ? newHashSet(destFiles) : Collections.<String>emptySet();

        for (String name : srcFiles) {
            final String filePath;
            if (path == null) {
                filePath = name;
            } else {
                filePath = path + "/" + name;
            }

            final File file = new File(srcDir, filePath);
            if (!file.isDirectory()) {
                if (destFileSet.contains(file.getAbsolutePath())) {
                    destFileSet.remove(file.getAbsolutePath());
                    visitCommon(filePath, visitor);
                } else {
                    visitOnlyInSrc(filePath, visitor);
                }
            } else {
                // recurse
                scan(filePath, visitor);
            }
        }

        for (String s : destFileSet) {
            visitOnlyInDest(s, visitor);
        }
    }

    private void visitOnlyInDest(String path, Visitor visitor) throws IOException {
        visitor.onlyInDest(path);
    }

    private void visitOnlyInSrc(String path, Visitor visitor) throws IOException {
        visitor.onlyInSrc(path);
    }

    private void visitCommon(String path, Visitor visitor) throws IOException {
        visitor.common(path);
    }

}
