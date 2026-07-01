package org.openclover.idea.util.psi;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.intellij.testFramework.LightIdeaTestCase;

public class PsiUtilIdeaTest extends LightIdeaTestCase {

    public void testFindClasses() throws Exception {
        // create a .java file directly (avoids dependency on Java file templates which
        // may not be available when the Java plugin is loaded from the classpath in tests)
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            try {
                VirtualFile file = getSourceRoot().createChildData(null, "TheClass.java");
                VfsUtil.saveText(file, "public class TheClass {}");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

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
