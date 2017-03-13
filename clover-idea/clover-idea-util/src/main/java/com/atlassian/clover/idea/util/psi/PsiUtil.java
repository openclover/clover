package com.atlassian.clover.idea.util.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;


public class PsiUtil {
    private PsiUtil() {
    }

    public static PsiClass[] findClasses(String name, Project project) {
        final GlobalSearchScope searchScope = GlobalSearchScope.projectScope(project);
        return JavaPsiFacade.getInstance(project).findClasses(name, searchScope);
    }

    public static PsiPackage getPackage(PsiDirectory directory) {
        return JavaDirectoryService.getInstance().getPackage(directory);
    }
}
