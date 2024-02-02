package com.atlassian.clover.idea.util.vfs;

import com.atlassian.clover.idea.util.psi.PsiUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;

import java.io.File;
import java.net.URL;

/**
 * Utility class to help work with the openapis' vfs package.
 */
public class VfsUtil extends com.intellij.openapi.vfs.VfsUtil {

    /**
     * Convert the specified file instance into an equivalent java.net.URL instance
     */
    public static URL convertToURL(final VirtualFile file) {
        if (file == null) {
            return null;
        }
        if (isFileInJar(file)) {
            // open api does not support looking up files within jars.
            return null;
        }
        Application app = ApplicationManager.getApplication();
        return (URL) app.runReadAction((Computable) () -> convertToURL(file.getUrl()));
    }

    /**
     * Convert the specified file instance into an java.io.File instance
     */
    public static File convertToFile(VirtualFile file) {
        if (file == null) {
            return null;
        }
        URL url = convertToURL(file);
        if (url == null) {
            return null;
        }
        return new File(url.getFile());
    }

    /**
     * Returns true if the file is located within a jar file.
     */
    public static boolean isFileInJar(VirtualFile file) {
        if (file == null) {
            return false;
        }
        String url = file.getUrl();
        return url.startsWith("jar");
    }


    /**
     * Retrieve the name of the virtual file relative to its source root.<p>
     * Path will be delimited with <code>/</code>.
     *
     * @param project base project
     * @param file    virtual file
     * @return slash path in source root or null when file is outside project scope.
     */
    public static String getRootRelativeFilename(Project project, VirtualFile file) {
        return getRootRelativeFilename(project, file, '/');
    }

    /**
     * Retrieve the name of the virtual file relative to its source root.<p>
     *
     * @param project       base project
     * @param file          virtual file
     * @param separatorChar path delimiter
     * @return path in source root or null when file is outside project scope.
     */
    public static String getRootRelativeFilename(Project project, VirtualFile file, char separatorChar) {
        final PsiDirectory psiDirectory;
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            psiDirectory = PsiManager.getInstance(project).findDirectory(file);
        } else {
            psiDirectory = psiFile.getContainingDirectory();
        }
        PsiPackage pkg = psiDirectory == null ? null : PsiUtil.getPackage(psiDirectory);
        if (pkg == null) {
            return null;
        }

        final String pkgStr = pkg.getQualifiedName();
        final String psiFileName = psiFile == null ? "" : psiFile.getName();

        return pkgStr.length() == 0 ? psiFileName : pkgStr.replace('.', separatorChar) + separatorChar + psiFileName;
    }

    /**
     * Retrieve path relative to project root or full path if given file is not a descendant of project root
     *
     * @param sourceVirtualFile the file
     * @param project           project
     * @return project root relative path
     */
    public static String calcRelativeToProjectPath(VirtualFile sourceVirtualFile, Project project) {
        final String relative = getRelativePath(sourceVirtualFile, project.getBaseDir(), '/');
        return relative != null ? relative : sourceVirtualFile.getPath();
    }
}
