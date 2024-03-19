package org.openclover.idea.build;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.mockito.Mockito;
import org.openclover.idea.ApplicationTestHelper;
import org.openclover.idea.config.IdeaCloverConfig;

import static org.mockito.Mockito.when;

public class InclusionDetectorIdeaTest extends PsiTestCase {
    private VirtualFile orgOpenCloverDir;
    private PsiClass psiClass;
    private VirtualFile contentRoot;

    private VirtualFile foreignRoot;
    private VirtualFile subDir;
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

    public void testProcessFile() {
        final IdeaCloverConfig config = Mockito.mock(IdeaCloverConfig.class);
        when(config.getIncludes()).thenReturn(null);
        when(config.getExcludes()).thenReturn(null);

        //Disabled
        InclusionDetector result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isCloverDisabled());

        when(config.isEnabled()).thenReturn(true);
        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isCloverDisabled());

        // Defaults
        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), orgOpenCloverDir, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), nonJava.getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isNotJava());

        result = ProjectInclusionDetector.processFile(getProject(), foreignRoot, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isModuleNotFound());

        result = ProjectInclusionDetector.processFile(getProject(), subDir, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isModuleNotFound());

        result = ProjectInclusionDetector.processFile(getProject(), foreignFile.getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isModuleNotFound());

        // exclude file

        when(config.getExcludes()).thenReturn("org/openclover/SomeClass.java");
        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), orgOpenCloverDir, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());


        // exclude dir
        when(config.getExcludes()).thenReturn("org/openclover/*.java");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());
        
//        result = ProjectInclusionDetector.processFile(getProject(), atlassian, true, config, fMgr);
//        assertFalse(result.isIncluded());
//        assertTrue(result.isPatternExcluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        // exclude dir recursively
        when(config.getExcludes()).thenReturn("org/openclover/**");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), orgOpenCloverDir, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        // exclude dir
        when(config.getExcludes()).thenReturn("org/*.java");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), orgOpenCloverDir, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertTrue(result.isIncluded());

        // exclude dir recursively
        when(config.getExcludes()).thenReturn("org/**");

        result = ProjectInclusionDetector.processFile(getProject(), contentRoot, true, config);
        assertTrue(result.isIncluded());

        result = ProjectInclusionDetector.processFile(getProject(), orgOpenCloverDir, true, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());

        result = ProjectInclusionDetector.processFile(getProject(), psiClass.getContainingFile().getVirtualFile(), false, config);
        assertFalse(result.isIncluded());
        assertTrue(result.isPatternExcluded());
    }
}
