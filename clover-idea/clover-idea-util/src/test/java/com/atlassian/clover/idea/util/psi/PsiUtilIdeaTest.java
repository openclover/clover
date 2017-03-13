package com.atlassian.clover.idea.util.psi;

import com.atlassian.clover.idea.util.psi.PsiUtil;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.intellij.testFramework.LightIdeaTestCase;

public class PsiUtilIdeaTest extends LightIdeaTestCase {

    public void testFindClasses() throws Exception {
        // create a class
        JavaDirectoryService.getInstance().createClass(
                getPsiManager().findDirectory(getSourceRoot()),
                "TheClass");

        // and find it
        final PsiClass[] cls = PsiUtil.findClasses("TheClass", getProject());
        assertNotNull(cls);
        assertEquals(cls.length, 1);
        assertEquals("TheClass", cls[0].getName());
    }

    public void testGetPackage() {
        final PsiDirectory dir = getPsiManager().findDirectory(getSourceRoot());
        final PsiPackage pkg = PsiUtil.getPackage(dir);
        assertNotNull(pkg);
    }

}
