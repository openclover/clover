package com.atlassian.clover.idea.build;

import com.atlassian.clover.idea.ApplicationTestHelper;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import org.mockito.Mockito;

public class InclusionDetectorIdeaTest extends PsiTestCase {
    private VirtualFile atlassian;
    private PsiClass psiClass;
    private VirtualFile contentRoot;

    private VirtualFile foreignRoot;
    private VirtualFile subdir;
    private PsiFile foreignFile;
    private PsiFile nonJava;

    @Override
    protected void setUp() throws Exception {
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

    public void testProcessFile() throws Exception {
        final IdeaCloverConfig config = Mockito.mock(IdeaCloverConfig.class);
        Mockito.when(config.getIncludes()).thenReturn(null);
        Mockito.when(config.getExcludes()).thenReturn(null);

        //Disabled
        InclusionDetector result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isCloverDisabled());

        Mockito.when(config.isEnabled()).thenReturn(true);
        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isCloverDisabled());

        // Defaults
        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), atlassian, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), nonJava.getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isNotJava());

        result = ProjectInclusionDetector.processFile(getProject(), foreignRoot, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isModuleNotFound());

        result = ProjectInclusionDetector.processFile(getProject(), subdir, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isModuleNotFound());

        result = ProjectInclusionDetector.processFile(getProject(), foreignFile.getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isModuleNotFound());

        // exclude file

        Mockito.when(config.getExcludes()).thenReturn("com/atlassian/SomeClass.java");
        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), atlassian, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());


        // exclude dir
        Mockito.when(config.getExcludes()).thenReturn("com/atlassian/*.java");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());
        
//        result = ProjectInclusionDetector.processFile(getProject(), atlassian, true, config, fMgr);
//        assertFalse(result.isIncluded());
//        assertTrue(result.isPatternExcluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        // exclude dir recursively
        Mockito.when(config.getExcludes()).thenReturn("com/atlassian/**");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), atlassian, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        // exclude dir
        Mockito.when(config.getExcludes()).thenReturn("com/*.java");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), atlassian, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertTrue(result.isIncluded());

        // exclude dir recursively
        Mockito.when(config.getExcludes()).thenReturn("com/**");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), atlassian, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

    }
}
