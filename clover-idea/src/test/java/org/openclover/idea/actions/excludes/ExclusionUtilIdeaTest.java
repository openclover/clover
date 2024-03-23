package org.openclover.idea.actions.excludes;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.openclover.idea.ApplicationTestHelper;
import org.openclover.idea.util.psi.PsiUtil;

public class ExclusionUtilIdeaTest extends PsiTestCase {
    private VirtualFile orgOpenCloverDir;
    private PsiClass psiClass;
    private VirtualFile contentRoot;
    private VirtualFile foreignRoot;
    private VirtualFile subDir;
    private PsiFile foreignFile;
    private PsiFile nonJava;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ApplicationTestHelper.runWriteAction(new ApplicationTestHelper.Action() {
            @Override
            public void run() throws Exception {
                final Module m = getModule();
                contentRoot = getVirtualFile(createTempDirectory());

                PsiTestUtil.addSourceRoot(m, contentRoot);
                orgOpenCloverDir = contentRoot.createChildDirectory(this, "org").createChildDirectory(this, "openclover");

                final PsiFile psiFile = createFile(m, orgOpenCloverDir, "SomeClass.java", "package org.openclover;\npublic class SomeClass {}");
                psiClass = (PsiClass) psiFile.getChildren()[3];
                nonJava = createFile(getModule(), orgOpenCloverDir, "NonJava.txt", "non java file");

                foreignRoot = getVirtualFile(createTempDirectory());
                subDir = foreignRoot.createChildDirectory(this, "org").createChildDirectory(this, "openclover");
                foreignFile = createFile("ForeignFile.java", "package org.openclover;\\npublic class ForeignFile {}");
                foreignFile.getVirtualFile().move(this, subDir);
            }
        });
    }

    public void testIsEnabled() {
        assertTrue(psiClass.getContainingFile().getVirtualFile().getPath().endsWith("org/openclover/SomeClass.java"));

        final Project project = getProject();
        assertTrue(ExclusionUtil.isEnabled(getPsiManager().findDirectory(contentRoot), project));
        assertTrue(ExclusionUtil.isEnabled(getPsiManager().findDirectory(orgOpenCloverDir), project));
        assertTrue(ExclusionUtil.isEnabled(psiClass.getContainingFile(), project));
        PsiPackage pkg = PsiUtil.getPackage(psiClass.getContainingFile().getContainingDirectory());
        assertTrue(ExclusionUtil.isEnabled(pkg, project));

        assertFalse(ExclusionUtil.isEnabled(getPsiManager().findDirectory(foreignRoot), project));
        assertFalse(ExclusionUtil.isEnabled(getPsiManager().findDirectory(subDir), project));
        assertFalse(ExclusionUtil.isEnabled(nonJava, project));
        assertFalse(ExclusionUtil.isEnabled(foreignFile, project));
    }

    public void testGetPattern() {
        assertEquals("org/openclover/SomeClass.java", ExclusionUtil.getPattern(psiClass));
        assertNull(ExclusionUtil.getRecursivePattern(psiClass));

        final PsiDirectory pdAtlassian = getPsiManager().findDirectory(orgOpenCloverDir);
        assertEquals("org/openclover/*.java", ExclusionUtil.getPattern(pdAtlassian));
        assertEquals("org/openclover/**", ExclusionUtil.getRecursivePattern(pdAtlassian));

        final PsiDirectory root = getPsiManager().findDirectory(contentRoot);
        assertEquals("*.java", ExclusionUtil.getPattern(root));
        assertEquals("**", ExclusionUtil.getRecursivePattern(root));

        PsiPackage pkg = PsiUtil.getPackage(psiClass.getContainingFile().getContainingDirectory());
        assertEquals("org/openclover/*.java", ExclusionUtil.getPattern(pkg));
        assertEquals("org/openclover/**", ExclusionUtil.getRecursivePattern(pkg));

        assertNull(ExclusionUtil.getPattern(getPsiManager().findDirectory(subDir)));
        assertNull(ExclusionUtil.getRecursivePattern(getPsiManager().findDirectory(subDir)));
        assertNull(ExclusionUtil.getPattern(getPsiManager().findDirectory(foreignRoot)));
        assertNull(ExclusionUtil.getRecursivePattern(getPsiManager().findDirectory(foreignRoot)));
        assertNull(ExclusionUtil.getPattern(nonJava));
        assertNull(ExclusionUtil.getRecursivePattern(nonJava));
        assertNull(ExclusionUtil.getPattern(foreignFile));
        assertNull(ExclusionUtil.getRecursivePattern(foreignFile));
    }

}
