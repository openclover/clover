package com.atlassian.clover.idea.actions.excludes;

import com.atlassian.clover.idea.util.vfs.VfsUtil;
import com.atlassian.clover.idea.util.InclusionUtil;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.openclover.util.Lists.newArrayList;

public abstract class ExclusionUtil {
    static VirtualFile getVirtualFile(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        final VirtualFile virtualFile;
        final PsiFile psiFile = element.getContainingFile();
        if (psiFile != null) {
            virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null || !FileTypeManager.getInstance().getFileTypeByFile(virtualFile).equals(StdFileTypes.JAVA)) {
                return null;
            }
        } else if (element instanceof PsiDirectory) {
            virtualFile = ((PsiDirectory) element).getVirtualFile();
        } else {
            return null;
        }
        return virtualFile;

    }

    static boolean isEnabled(PsiElement element, Project project) {
        if (element instanceof PsiPackage) {
            return true;
        }
        final VirtualFile virtualFile = getVirtualFile(element);

        return virtualFile != null && ModuleUtil.findModuleForFile(virtualFile, project) != null;
    }

    public static String getPattern(@Nullable PsiElement element) {
        if (element instanceof PsiPackage) {
            return ((PsiPackage)element).getQualifiedName().replace('.', '/') + "/*.java";
        }
        final VirtualFile virtualFile = ExclusionUtil.getVirtualFile(element);
        if (element == null || virtualFile == null) {
            return null;
        }
        final Project project = element.getProject();
        final String relativePath = VfsUtil.getRootRelativeFilename(project, virtualFile);

        return relativePath == null ? null : (virtualFile.isDirectory() ? relativePath + "*.java" : relativePath);
    }

    public static String getRecursivePattern(@Nullable PsiElement element) {
        if (element instanceof PsiPackage) {
            return ((PsiPackage)element).getQualifiedName().replace('.', '/') + "/**";
        }
        final VirtualFile virtualFile = ExclusionUtil.getVirtualFile(element);
        if (element == null || virtualFile == null || !virtualFile.isDirectory()) {
            return null;
        }
        final Project project = element.getProject();
        final String relativePath = VfsUtil.getRootRelativeFilename(project, virtualFile);

        return relativePath == null ? null : relativePath + "**";
    }

    public static boolean isExplicitlyIncluded(String config, String pattern) {
        final String[] includes = InclusionUtil.toArray(config, " ,");
        for (String include : includes) {
            if (include.equals(pattern)) {
                return true;
            }
        }
        return false;
        
    }

    public static String removePattern(String existing, String pattern) {
        boolean modified = false;
        final List<String> tokens = newArrayList(InclusionUtil.toArray(existing, " ,"));
        Iterator<String> i = tokens.iterator();
        while (i.hasNext()) {
            final String token = i.next();
            if (token.trim().equals(pattern)) {
                modified = true;
                i.remove();
            }
        }
        if (modified) {
            String separator = "";
            StringBuilder sb = new StringBuilder();
            for (String token : tokens) {
                sb.append(separator);
                sb.append(token);
                separator = ", ";
            }
            return sb.toString();
        } else {
            return existing;
        }
    }

    public static String getDisplayName(@NotNull String pattern) {
        final int idx = pattern.indexOf('*');
        final String name = idx == -1 ? pattern : pattern.substring(0, idx);

        return name.length() > 0 ? name : "<default>";
    }
}
