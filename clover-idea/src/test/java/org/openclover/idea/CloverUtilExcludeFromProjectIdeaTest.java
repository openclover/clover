package org.openclover.idea;

import org.openclover.idea.util.ProjectUtil;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestCase;

import java.io.IOException;

public class CloverUtilExcludeFromProjectIdeaTest extends IdeaTestCase {
    private VirtualFile dir1;
    private VirtualFile dir2;
    private VirtualFile subdir1;
    private VirtualFile subdir2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dir1 = getVirtualFile(createTempDirectory());
        dir2 = getVirtualFile(createTempDirectory());

        ApplicationTestHelper.runWriteAction(() -> {
            subdir1 = dir1.createChildDirectory(null, "subdir1");
            subdir2 = dir1.createChildDirectory(null, "subdir2");

            final ModuleRootManager rManager = ModuleRootManager.getInstance(getModule());
            final ModifiableRootModel rootModel = rManager.getModifiableModel();
            rootModel.addContentEntry(dir1);
            rootModel.addContentEntry(dir2);
            rootModel.commit();
        });
    }

    public void testSubdir() throws Exception {
        assertFolderExcluded(subdir1);
    }

    public void testRootDir() throws Exception {
        assertFolderExcluded(dir1);
    }

    protected void assertFolderExcluded(final VirtualFile dir) throws Exception {
        ApplicationTestHelper.runWriteAction(() -> {
            //precondition
            assertEquals(0, ModuleRootManager.getInstance(getModule()).getExcludeRoots().length);

            boolean changed = ProjectUtil.excludeFromProject(getProject(), dir);
            assertTrue(changed);

            final VirtualFile[] excludes = ModuleRootManager.getInstance(getModule()).getExcludeRoots();
            assertEquals(1, excludes.length);
            assertEquals(dir, excludes[0]);
        });
    }
    public void testUnrelatedDir() throws IOException {
        //precondition
        assertEquals(0, ModuleRootManager.getInstance(getModule()).getExcludeRoots().length);

        final VirtualFile unrelatedDir = getVirtualFile(createTempDirectory());
        boolean changed = ProjectUtil.excludeFromProject(getProject(), unrelatedDir);
        assertFalse(changed);

        final VirtualFile[] excludes = ModuleRootManager.getInstance(getModule()).getExcludeRoots();
        assertEquals(0, excludes.length);
    }
    
    public void testAlreadyExcluded() throws Exception {
        ApplicationTestHelper.runWriteAction(() -> {
            //precondition
            assertEquals(0, ModuleRootManager.getInstance(getModule()).getExcludeRoots().length);
            ProjectUtil.excludeFromProject(getProject(), subdir1);
            final VirtualFile[] excludesPrecondition = ModuleRootManager.getInstance(getModule()).getExcludeRoots();
            assertEquals(1, excludesPrecondition.length);
            assertEquals(subdir1, excludesPrecondition[0]);

            // exclude the same dir again
            boolean changed = ProjectUtil.excludeFromProject(getProject(), subdir1);
            assertFalse(changed);

            final VirtualFile[] excludes = ModuleRootManager.getInstance(getModule()).getExcludeRoots();
            assertEquals(1, excludes.length);
            assertEquals(subdir1, excludes[0]);
        });
    }

}
