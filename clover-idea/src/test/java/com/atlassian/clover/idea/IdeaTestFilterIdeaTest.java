package com.atlassian.clover.idea;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestCase;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

public class IdeaTestFilterIdeaTest extends IdeaTestCase {
    private VirtualFile src;
    private VirtualFile test;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ApplicationTestHelper.runWriteAction(new ApplicationTestHelper.Action() {
            @Override
            public void run() throws Exception {
                final Module module = createModule("test module");
                final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();

                final ContentEntry contentEntry;
                try {
                    contentEntry = model.addContentEntry(module.getModuleFile().getParent().createChildDirectory(null, "root"));
                    src = contentEntry.getFile().createChildDirectory(null, "src");
                    test = contentEntry.getFile().createChildDirectory(null, "test");
                } catch (IOException e) {
                    model.dispose();
                    throw e;
                }

                contentEntry.addSourceFolder(src, false);
                contentEntry.addSourceFolder(test, true);
                model.commit();
            }
        });
    }

    @Override
    protected void tearDown() throws Exception {
        src = null;
        test = null;
        super.tearDown();
    }

    public void testIsInTestSource() throws Exception {
        ApplicationTestHelper.runWriteAction(new ApplicationTestHelper.Action() {
            @Override
            public void run() throws Exception {
                final VirtualFile srcFile = src.createChildData(null, "File1.java");
                final VirtualFile testFile = test.createChildData(null, "File2.java");

                IdeaTestFilter filter = new IdeaTestFilter(getProject());

                assertFalse(filter.isInTestFolder(VfsUtil.virtualToIoFile(srcFile)));
                assertTrue(filter.isInTestFolder(VfsUtil.virtualToIoFile(testFile)));
            }
        });
    }

    public void testAccept() throws Exception {
        ApplicationTestHelper.runWriteAction(new ApplicationTestHelper.Action() {
            @Override
            public void run() throws Exception {
                final VirtualFile srcFile = src.createChildData(null, "File1.java");
                final VirtualFile testFile = test.createChildData(null, "File2.java");
                final VirtualFile testInSrcFile = src.createChildData(null, "FileTest.java");

                IdeaTestFilter filter = new IdeaTestFilter(getProject());

                HasMetrics methodInfo = Mockito.mock(FullMethodInfo.class);
                HasMetrics packageInfo = Mockito.mock(FullPackageInfo.class);

                ClassInfo classInSrc = createClassInfo(srcFile, false);
                ClassInfo classInTest = createClassInfo(testFile, false);
                ClassInfo testClassInSrc = createClassInfo(testInSrcFile, true);


                assertTrue(filter.accept(methodInfo));
                assertTrue(filter.accept(packageInfo));

                assertTrue(filter.accept(classInTest));
                assertTrue(filter.accept(testClassInSrc));
                assertFalse(filter.accept(classInSrc));

                filter = filter.invert();

                assertTrue(filter.accept(methodInfo));
                assertTrue(filter.accept(packageInfo));

                assertFalse(filter.accept(classInTest));
                assertFalse(filter.accept(testClassInSrc));
                assertTrue(filter.accept(classInSrc));
            }
        });
    }


//    public void testReadAccessNecessary() throws Exception {
//        final VirtualFile testFile = test.createChildData(null, "File2.java");
//        final IdeaTestFilter filter = new IdeaTestFilter(getProject());
//
//        ApplicationManager.getApplication().
//
//        assertTrue(filter.isInTestFolder(VfsUtil.virtualToIoFile(testFile)));
//
//        final ModifiableModuleModel moduleModel = ModuleManager.getInstance(getProject()).getModifiableModel();
//        Module module = moduleModel.findModuleByName("test module");
//        moduleModel.renameModule(module, "test module renamed");
//
//        final Exception[] exceptionHolder = new Exception[1];
//
//        ApplicationManager.getApplication().runReadAction(new Runnable() {
//
//            public void run() {
//                ModuleManagerImpl.commitModelWithRunnable(moduleModel, new Runnable() {
//                    public void run() {
//                        try {
//                            final Future<Exception> future = Executors.newSingleThreadExecutor().submit(new Callable<Exception>() {
//
//                                public Exception call() throws Exception {
//                                    try {
//                                        assertFalse(ApplicationManager.getApplication().isDispatchThread());
//                                        assertFalse(ApplicationManager.getApplication().isReadAccessAllowed());
//                                        assertTrue(filter.isInTestFolder(VfsUtil.virtualToIoFile(testFile)));
//                                    } catch (Exception ex) {
//                                        return ex;
//                                    }
//                                    return null;
//                                }
//                            });
//                            exceptionHolder[0] = future.get(10, TimeUnit.SECONDS);
//                        } catch (Exception e) {
//                            exceptionHolder[0] = e;
//                        }
//                    }
//                });
//            }
//        });
//        if (exceptionHolder[0] != null) {
//            throw exceptionHolder[0];
//        }
//    }

    public void testFailedPhysicalFile() {
        FullFileInfo missingFile = Mockito.mock(FullFileInfo.class, "FullFileInfo for missing file");
        Mockito.when(missingFile.getPhysicalFile()).thenReturn(new File("/tmp/non/existent/File.java"));

        FullClassInfo classInMissing = Mockito.mock(FullClassInfo.class);
        Mockito.when(classInMissing.getContainingFile()).thenReturn(missingFile);

        FullFileInfo missingTestFile = Mockito.mock(FullFileInfo.class, "FullFileInfo for missing file");
        Mockito.when(missingTestFile.getPhysicalFile()).thenReturn(new File("/tmp/non/existent/FileTest.java"));

        FullClassInfo testClassInMissing = Mockito.mock(FullClassInfo.class);
        Mockito.when(testClassInMissing.getContainingFile()).thenReturn(missingTestFile);
        Mockito.when(missingTestFile.isTestFile()).thenReturn(true);

        IdeaTestFilter filter = new IdeaTestFilter(getProject());
        assertFalse(filter.accept(classInMissing));
        assertTrue(filter.accept(testClassInMissing));
    }

    private static FullClassInfo createClassInfo(VirtualFile srcFile, boolean forceTest) {
        FullFileInfo fileInSrc = Mockito.mock(FullFileInfo.class);
        Mockito.when(fileInSrc.getPhysicalFile()).thenReturn(VfsUtil.virtualToIoFile(srcFile));
        if (forceTest) {
            Mockito.when(fileInSrc.isTestFile()).thenReturn(true);
        }

        FullClassInfo classInSrc = Mockito.mock(FullClassInfo.class);
        Mockito.when(classInSrc.getContainingFile()).thenReturn(fileInSrc);
        return classInSrc;
    }


}
