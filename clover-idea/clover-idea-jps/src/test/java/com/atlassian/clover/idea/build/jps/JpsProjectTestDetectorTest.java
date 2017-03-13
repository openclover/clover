package com.atlassian.clover.idea.build.jps;

import com.atlassian.clover.instr.java.JavaMethodContext;
import com.atlassian.clover.instr.java.JavaTypeContext;
import com.atlassian.clover.instr.tests.DefaultTestDetector;
import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.spi.lang.Language;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifiers;
import org.jetbrains.jps.devkit.model.JpsPluginModuleProperties;
import org.jetbrains.jps.devkit.model.JpsPluginModuleType;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.impl.JpsModelImpl;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.module.JpsModule;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link JpsProjectTestDetector}
 */
public class JpsProjectTestDetectorTest {

    protected JpsProject sampleProject;

    @Before
    public void setUp() {
        sampleProject = createSampleProject();
    }

    /**
     * @see JpsProjectTestDetector#isTypeMatch(com.atlassian.clover.instr.tests.TestDetector.SourceContext,
     *      com.atlassian.clover.instr.tests.TestDetector.TypeContext)
     */
    @Test
    public void testIsTypeMatch() {
        final JpsProjectTestDetector detector = new JpsProjectTestDetector(sampleProject,
                JpsProjectPrefixTreeUtil.collectRootTypes(sampleProject));
        final TestDetector.SourceContext appSourceContext =
                createSourceContext(FileUtils.getPlatformSpecificPath("Project/ModuleA/src/MyClass.java"));
        final TestDetector.SourceContext testSourceContext =
                createSourceContext(FileUtils.getPlatformSpecificPath("Project/ModuleA/test/MyTest.java"));
        final TestDetector.TypeContext junitClass = new JavaTypeContext(new HashMap(), new Modifiers(),
                "com.acme", "MyTest", "org.junit.TestCase");
        final TestDetector.TypeContext normalClass = new JavaTypeContext(new HashMap(), new Modifiers(),
                "com.acme", "MyClass", "java.lang.Object");

        // case: class is a JUnit class, located in /src folder -> isTypeMatch returns true
        assertTrue(detector.isTypeMatch(appSourceContext, junitClass));

        // case: class is not a JUnit class, located in /src folder -> isTypeMatch returns false
        assertFalse(detector.isTypeMatch(appSourceContext, normalClass));

        // case: class is not a JUnit class, but located in /test folder -> isTypeMatch returns true
        assertTrue(detector.isTypeMatch(testSourceContext, normalClass));
    }

    /**
     * @see JpsProjectTestDetector#isMethodMatch(com.atlassian.clover.instr.tests.TestDetector.SourceContext,
     *      com.atlassian.clover.instr.tests.TestDetector.MethodContext)
     */
    @Test
    public void testIsMethodMatch() {
        final TestDetector.SourceContext testSourceContext =
                createSourceContext(FileUtils.getPlatformSpecificPath("Project/ModuleA/test/MyTest.java"));
        final TestDetector.MethodContext testMethod = JavaMethodContext.createFor(
                new MethodSignature("testSomething", null, "void", null, null, Modifiers.createFrom(Modifier.PUBLIC, null)));
        final TestDetector.MethodContext nonTestMethod = JavaMethodContext.createFor(
                new MethodSignature("helloWorld"));

        final DefaultTestDetector defaultTestDetector = new DefaultTestDetector();
        final JpsProjectTestDetector jpsTestDetector = new JpsProjectTestDetector(sampleProject,
                JpsProjectPrefixTreeUtil.collectRootTypes(sampleProject));

        // no JPS-specific implementation so just compare with a default test detector
        assertEquals(
                defaultTestDetector.isMethodMatch(testSourceContext, testMethod),
                jpsTestDetector.isMethodMatch(testSourceContext, testMethod));
        assertEquals(
                defaultTestDetector.isMethodMatch(testSourceContext, nonTestMethod),
                jpsTestDetector.isMethodMatch(testSourceContext, nonTestMethod));
    }

    /**
     * @see JpsProjectTestDetector#isInTestFolder(java.io.File)
     */
    @Test
    public void testIsInTestFolder() {
        final JpsProjectTestDetector detector = new JpsProjectTestDetector(sampleProject,
                JpsProjectPrefixTreeUtil.collectRootTypes(sampleProject));

        // app source folder
        assertFalse(detector.isInTestFolder(new File(FileUtils.getPlatformSpecificPath(
                "Project/ModuleA/src/FooTest.java"))));

        // test source folder
        assertTrue(detector.isInTestFolder(new File(FileUtils.getPlatformSpecificPath(
                "Project/ModuleA/test/FooTest.java"))));

        // outside known source roots
        assertFalse(detector.isInTestFolder(new File(FileUtils.getPlatformSpecificPath(
                "Project/ModuleXYZ/src/FooTest.java"))));
    }

    /**
     * Creates a sample project with a following structure:
     *
     * <pre>
     *    Project
     *      ModuleA   - JAVA_MODULE
     *        src     - app source root
     *        test    - test source root
     *      ModuleB   - PLUGIN_MODULE
     *        src     - app source root
     *        test    - test source root
     * </pre>
     *
     * @return JpsProject
     */
    protected JpsProject createSampleProject() {
        final JpsProject jpsProject = new JpsModelImpl(null).getProject();
        final JpsElementFactory factory = JpsElementFactory.getInstance();

        // ModuleA
        final JpsModule moduleA = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE,
                factory.createDummyElement());
        moduleA.addSourceRoot("file://Project/ModuleA/src", JavaSourceRootType.SOURCE);
        moduleA.addSourceRoot("file://Project/ModuleA/test", JavaSourceRootType.TEST_SOURCE);
        jpsProject.addModule(moduleA);

        // ModuleB
        final JpsModule moduleB = factory.createModule("ModuleB", JpsPluginModuleType.INSTANCE,
                factory.createSimpleElement(
                        new JpsPluginModuleProperties("file://plugin.xml", "file://MANIFEST.MF")));
        moduleB.addSourceRoot("file://Project/ModuleB/src", JavaSourceRootType.SOURCE);
        moduleB.addSourceRoot("file://Project/ModuleB/test", JavaSourceRootType.TEST_SOURCE);
        jpsProject.addModule(moduleB);

        return jpsProject;
    }

    /**
     * Returns a stub of a source context
     *
     * @param pathToFile file name
     * @return TestDetector.SourceContext
     */
    protected TestDetector.SourceContext createSourceContext(final String pathToFile) {
        return new TestDetector.SourceContext() {
            @Override
            public Language getLanguage() {
                return Language.Builtin.JAVA;
            }

            @Override
            public boolean areAnnotationsSupported() {
                return true;
            }

            @Override
            public File getSourceFile() {
                return new File(pathToFile);
            }
        };
    }
}
