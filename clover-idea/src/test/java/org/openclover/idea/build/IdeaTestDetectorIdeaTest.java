package org.openclover.idea.build;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestCase;
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig;
import org.openclover.core.instr.java.FileStructureInfo;
import org.openclover.core.instr.java.InstrumentationState;
import org.openclover.core.instr.java.JavaMethodContext;
import org.openclover.core.instr.java.JavaTypeContext;
import org.openclover.core.instr.tests.DefaultTestDetector;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.MethodSignature;
import org.openclover.idea.ApplicationTestHelper;
import org.openclover.idea.util.vfs.VfsUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class IdeaTestDetectorIdeaTest extends IdeaTestCase {
    private VirtualFile src;
    private VirtualFile test;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ApplicationTestHelper.runWriteAction(() -> {
            final Module module = createModule("test module");
            final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();

            final ContentEntry contentEntry;
            try {
                contentEntry = model.addContentEntry(module.getModuleFile().getParent().createChildDirectory(null, "root"));
                src = contentEntry.getFile().createChildDirectory(null, "src");
                test = contentEntry.getFile().createChildDirectory(null, "test");
            } catch (IOException e){
                model.dispose();
                throw e;
            }

            contentEntry.addSourceFolder(src, false);
            contentEntry.addSourceFolder(test, true);
            model.commit();
        });
    }

    @Override
    protected void tearDown() throws Exception {
        src = null;
        test = null;
        super.tearDown();
    }


    public void testIsClassMatch() throws Exception {
        ApplicationTestHelper.runWriteAction(() -> {
            final VirtualFile srcFile = src.createChildData(null, "File1.java");
            final VirtualFile testFile = test.createChildData(null, "File2.java");

            IdeaTestDetector detector = new IdeaTestDetector(getProject());
            DefaultTestDetector defaultDetector = new DefaultTestDetector();

            InstrumentationState state = newInstrState(srcFile, null);
            // check preconditions
            assertTrue(defaultDetector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File1", "TestCase")));
            assertFalse(defaultDetector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File1", "NotTestCase")));
            assertTrue(defaultDetector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File2", "TestCase")));
            assertFalse(defaultDetector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File2", "NotTestCase")));

            FullFileInfo fileInfo1 = new FullFileInfo(null, VfsUtil.convertToFile(srcFile), null, 0, 0, 0, 0, 0, 0, 0);
            FullFileInfo fileInfo2 = new FullFileInfo(null, VfsUtil.convertToFile(testFile), null, 0, 0, 0, 0, 0, 0, 0);
            // actual check
            state = newInstrState(srcFile, fileInfo1);
            assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File1", "TestCase")));
            assertFalse(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File1", "NotTestCase")));

            state = newInstrState(srcFile, fileInfo2);
            assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File2", "TestCase")));
            assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File2", "NotTestCase")));
        });
    }

    public void testIsMethodMatch() throws Exception {
        ApplicationTestHelper.runWriteAction(() -> {
            final VirtualFile testFile = test.createChildData(null, "File2.java");
            IdeaTestDetector detector = new IdeaTestDetector(getProject());
            FullFileInfo fileInfo = new FullFileInfo(null, VfsUtil.convertToFile(testFile), null, 0, 0, 0, 0, 0, 0, 0);
            InstrumentationState state = newInstrState(testFile, fileInfo);

            assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "File2", "NotTestCase"))); // Idea-specific matcher triggered

            final MethodSignature constructor = new MethodSignature(null, null, null, "Constructor", null, "", null, null);
            constructor.getModifiers().setMask(Modifier.PUBLIC);

            final MethodSignature method = new MethodSignature(null, null, null, "testMethod", null, "void", null, null);
            method.getModifiers().setMask(Modifier.PUBLIC);

            assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(constructor)));
            assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(method)));
        });
    }

    private InstrumentationState newInstrState(VirtualFile testFile, FullFileInfo fileInfo) {
        return new InstrumentationState(
            null, fileInfo, new FileStructureInfo(new File(testFile.getPath())),
            new JavaInstrumentationConfig());
    }
}
