package com.atlassian.clover.idea.actions.excludes;

import com.atlassian.clover.idea.ApplicationTestHelper;
import com.atlassian.clover.idea.util.psi.PsiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;

public class ExclusionUtilIdeaTest extends PsiTestCase {
    private VirtualFile atlassian;
    private PsiClass psiClass;
    private VirtualFile contentRoot;
    private VirtualFile foreignRoot;
    private VirtualFile subdir;
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
                atlassian = contentRoot.createChildDirectory(this, "com").createChildDirectory(this, "atlassian");

                final PsiFile psiFile = createFile(m, atlassian, "SomeClass.java", "package com.atlassian;\npublic class SomeClass {}");
                psiClass = (PsiClass) psiFile.getChildren()[3];
                nonJava = createFile(getModule(), atlassian, "NonJava.txt", "non java file");

                foreignRoot = getVirtualFile(createTempDirectory());
                subdir = foreignRoot.createChildDirectory(this, "com").createChildDirectory(this, "atlassian");
                foreignFile = createFile("ForeignFile.java", "package com.atlassian;\\npublic class ForeignFile {}");
                foreignFile.getVirtualFile().move(this, subdir);
            }
        });
    }

    public void testIsEnabled() throws Exception {
        assertTrue(psiClass.getContainingFile().getVirtualFile().getPath().endsWith("com/atlassian/SomeClass.java"));

        final Project project = getProject();
        assertTrue(ExclusionUtil.isEnabled(getPsiManager().findDirectory(contentRoot), project));
        assertTrue(ExclusionUtil.isEnabled(getPsiManager().findDirectory(atlassian), project));
        assertTrue(ExclusionUtil.isEnabled(psiClass.getContainingFile(), project));
        PsiPackage pkg = PsiUtil.getPackage(psiClass.getContainingFile().getContainingDirectory());
        assertTrue(ExclusionUtil.isEnabled(pkg, project));

        assertFalse(ExclusionUtil.isEnabled(getPsiManager().findDirectory(foreignRoot), project));
        assertFalse(ExclusionUtil.isEnabled(getPsiManager().findDirectory(subdir), project));
        assertFalse(ExclusionUtil.isEnabled(nonJava, project));
        assertFalse(ExclusionUtil.isEnabled(foreignFile, project));

    }

    public void testGetPattern() throws Exception {
        assertEquals("com/atlassian/SomeClass.java", ExclusionUtil.getPattern(psiClass));
        assertNull(ExclusionUtil.getRecursivePattern(psiClass));

        final PsiDirectory pdAtlassian = getPsiManager().findDirectory(atlassian);
        assertEquals("com/atlassian/*.java", ExclusionUtil.getPattern(pdAtlassian));
        assertEquals("com/atlassian/**", ExclusionUtil.getRecursivePattern(pdAtlassian));

        final PsiDirectory root = getPsiManager().findDirectory(contentRoot);
        assertEquals("*.java", ExclusionUtil.getPattern(root));
        assertEquals("**", ExclusionUtil.getRecursivePattern(root));

        PsiPackage pkg = PsiUtil.getPackage(psiClass.getContainingFile().getContainingDirectory());
        assertEquals("com/atlassian/*.java", ExclusionUtil.getPattern(pkg));
        assertEquals("com/atlassian/**", ExclusionUtil.getRecursivePattern(pkg));

        assertNull(ExclusionUtil.getPattern(getPsiManager().findDirectory(subdir)));
        assertNull(ExclusionUtil.getRecursivePattern(getPsiManager().findDirectory(subdir)));
        assertNull(ExclusionUtil.getPattern(getPsiManager().findDirectory(foreignRoot)));
        assertNull(ExclusionUtil.getRecursivePattern(getPsiManager().findDirectory(foreignRoot)));
        assertNull(ExclusionUtil.getPattern(nonJava));
        assertNull(ExclusionUtil.getRecursivePattern(nonJava));
        assertNull(ExclusionUtil.getPattern(foreignFile));
        assertNull(ExclusionUtil.getRecursivePattern(foreignFile));
    }

}
