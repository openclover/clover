package com.atlassian.clover.idea.build.jps;

import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.idea.config.MappedCloverPluginConfig;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.impl.JpsModelImpl;
import org.jetbrains.jps.model.impl.JpsProjectImpl;
import org.jetbrains.jps.model.impl.JpsSimpleElementImpl;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaModuleType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.core.IsCollectionContaining;

import java.io.File;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link JpsModelUtil}
 */
public class JpsModelUtilTest {

    protected JpsElementFactory factory;

    @Before
    public void setUpClass() {
        factory = JpsElementFactory.getInstance();
    }

    /**
     * @see JpsModelUtil#isBuildWithCloverEnabled(org.jetbrains.jps.model.JpsProject)
     */
    @Test
    public void testIsBuildWithCloverEnabled() {
        // clover is disabled
        JpsProject jpsProject = createProjectWithCloverConfig(false, false);
        assertEquals(false, JpsModelUtil.isBuildWithCloverEnabled(jpsProject));

        // clover is enabled
        jpsProject = createProjectWithCloverConfig(false, true);
        assertEquals(true, JpsModelUtil.isBuildWithCloverEnabled(jpsProject));

        // clover config not found -> disabled
        jpsProject = createProjectWithoutCloverConfig();
        assertEquals(false, JpsModelUtil.isBuildWithCloverEnabled(jpsProject));
    }

    /**
     * @see JpsModelUtil#isCloverEnabled(org.jetbrains.jps.model.JpsProject)
     */
    @Test
    public void testIsCloverEnabled() {
        // clover is disabled
        JpsProject jpsProject = createProjectWithCloverConfig(false, false);
        assertEquals(false, JpsModelUtil.isCloverEnabled(jpsProject));

        // clover is enabled
        jpsProject = createProjectWithCloverConfig(true, false);
        assertEquals(true, JpsModelUtil.isCloverEnabled(jpsProject));

        // clover config not found -> disabled
        jpsProject = createProjectWithoutCloverConfig();
        assertEquals(false, JpsModelUtil.isCloverEnabled(jpsProject));
    }

    /**
     * @see JpsModelUtil#getCloverPluginConfig(org.jetbrains.jps.model.JpsProject)
     */
    @Test
    public void testGetCloverPluginConfig() {
        // with config
        JpsProject jpsProject = createProjectWithCloverConfig(true, true);
        assertNotNull(JpsModelUtil.getCloverPluginConfig(jpsProject));

        // no config
        jpsProject = createProjectWithoutCloverConfig();
        assertNull(JpsModelUtil.getCloverPluginConfig(jpsProject));
    }

    /**
     * @see JpsModelUtil#getExcludedRoots(org.jetbrains.jps.model.module.JpsModule)
     */
    @Test
    public void testGetExcludedRoots() {
        final JpsModule module = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE,
                factory.createDummyElement());
        module.getExcludeRootsList().addUrl("file://Project/ModuleA/src/excluded");
        module.getExcludeRootsList().addUrl("file://Project/ModuleA/src/excluded2");

        final Set<File> excludedRoots = JpsModelUtil.getExcludedRoots(module);
        assertEquals(2, excludedRoots.size());
        assertThat(excludedRoots, IsCollectionContaining.hasItem(new File(
                FileUtils.getPlatformSpecificPath("Project/ModuleA/src/excluded"))));
        assertThat(excludedRoots, IsCollectionContaining.hasItem(new File(
                FileUtils.getPlatformSpecificPath("Project/ModuleA/src/excluded2"))));

        assertThat(excludedRoots, not(IsCollectionContaining.hasItem(new File(
                FileUtils.getPlatformSpecificPath("Project/not/on/excluded/list")))));
    }

    /**
     * Test for {@link JpsModelUtil#findSourceRootForFile(org.jetbrains.jps.model.module.JpsModule, java.io.File)}
     * Case:
     * <pre>
     *     ModuleA
     *     + src
     *     + test
     *     + some_folder
     *       + Foo.java
     * </pre>
     */
    @Test
    public void testFindSourceRootForFile_NoAncestorRootFound() {
        final JpsModule module = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        module.addSourceRoot("file://ModuleA/src", JavaSourceRootType.SOURCE);
        module.addSourceRoot("file://ModuleA/test", JavaSourceRootType.TEST_SOURCE);

        // check a file outside root folder
        assertNull(JpsModelUtil.findSourceRootForFile(module, new File(new File("ModuleA", "some_folder"), "Foo.java")));
    }

    /**
     * Test for {@link JpsModelUtil#findSourceRootForFile(org.jetbrains.jps.model.module.JpsModule, java.io.File)}
     * Case:
     * <pre>
     *     ModuleA
     *     + src
     *       + Foo.java
     *     + test
     *       + FooTest.java
     * </pre>
     */
    @Test
    public void testFindSourceRootForFile_SeparateRoots() {
        final JpsModule module = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        module.addSourceRoot("file://ModuleA/src", JavaSourceRootType.SOURCE);
        module.addSourceRoot("file://ModuleA/test", JavaSourceRootType.TEST_SOURCE);

        // check file in src and test
        final JpsModuleSourceRoot srcRoot =
                JpsModelUtil.findSourceRootForFile(module, new File(new File("ModuleA", "src"), "Foo.java"));
        assertNotNull(srcRoot);
        assertEquals(JavaSourceRootType.SOURCE, srcRoot.getRootType());

        final JpsModuleSourceRoot testRoot =
                JpsModelUtil.findSourceRootForFile(module, new File(new File("ModuleA", "test"), "FooTest.java"));
        assertNotNull(testRoot);
        assertEquals(JavaSourceRootType.TEST_SOURCE, testRoot.getRootType());
    }

    /**
     * Test for {@link JpsModelUtil#findSourceRootForFile(org.jetbrains.jps.model.module.JpsModule, java.io.File)}
     * Case:
     * <pre>
     *     ModuleA
     *     + src                - SOURCE
     *       + Foo.java
     *       + test             - TEST_SOURCE
     *         + FooTest.java
     *       + acme
     *         + test           - TEST_SOURCE
     * </pre>
     */
    @Test
    public void testFindSourceRootForFile_OverlappingRoots() {
        final JpsModule module = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        module.addSourceRoot("file://ModuleA/src", JavaSourceRootType.SOURCE);
        module.addSourceRoot("file://ModuleA/src/test", JavaSourceRootType.TEST_SOURCE);
        module.addSourceRoot("file://ModuleA/src/acme/test", JavaSourceRootType.TEST_SOURCE);

        // check file in src
        final JpsModuleSourceRoot srcRoot =
                JpsModelUtil.findSourceRootForFile(module, new File(new File("ModuleA", "src"), "Foo.java"));
        assertNotNull(srcRoot);
        assertEquals(JavaSourceRootType.SOURCE, srcRoot.getRootType());

        final JpsModuleSourceRoot srcRoot2 =
                JpsModelUtil.findSourceRootForFile(module, new File(new File(new File("ModuleA", "src"), "acme"), "Foo.java"));
        assertNotNull(srcRoot2);
        assertEquals(JavaSourceRootType.SOURCE, srcRoot2.getRootType());

        // check file in src/test
        final JpsModuleSourceRoot testRoot =
                JpsModelUtil.findSourceRootForFile(module, new File(new File(new File("ModuleA", "src"), "test"), "FooTest.java"));
        assertNotNull(testRoot);
        assertEquals(JavaSourceRootType.TEST_SOURCE, testRoot.getRootType());
    }

    /**
     * Test for {@link JpsModelUtil#findSourceRootForFile(org.jetbrains.jps.model.module.JpsModule, java.io.File)}
     * <pre>
     *     ModuleA
     *     + src
     *       + Foo.java
     *       + excluded
     *         + test
     *           + FooExcluded.java
     * </pre>
     */
    @Test
    public void testFindSourceRootForFile_ExcludedFile() {
        final JpsModule module = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        module.addSourceRoot("file://ModuleA/src", JavaSourceRootType.SOURCE);
        module.addSourceRoot("file://ModuleA/src/excluded/test", JavaSourceRootType.TEST_SOURCE);
        module.getExcludeRootsList().addUrl("file://ModuleA/src/excluded");

        // check file in src - should be found
        final JpsModuleSourceRoot srcRoot = JpsModelUtil.findSourceRootForFile(
                module,
                new File(new File("ModuleA", "src"), "Foo.java"));
        assertNotNull(srcRoot);
        assertEquals(JavaSourceRootType.SOURCE, srcRoot.getRootType());

        // check file in src/test
        final JpsModuleSourceRoot testRoot = JpsModelUtil.findSourceRootForFile(
                module,
                new File(new File(new File(new File("ModuleA", "src"), "excluded"), "test"), "FooExcluded.java"));
        assertNull(testRoot);
    }

    /**
     * Test for {@link JpsModelUtil#findModuleForFile(org.jetbrains.jps.model.JpsProject, java.io.File)} Case: file
     * outside the module
     * <pre>
     *     ModuleA
     *     + src
     * </pre>
     */
    @Test
    public void testFindModuleForFile_NoModuleFound() {
        final JpsProject project = new JpsModelImpl(null).getProject();
        final JpsModule moduleA = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        moduleA.addSourceRoot("file://ModuleA/src", JavaSourceRootType.SOURCE);

        // search a file outside ModuleA
        final JpsModule moduleFound = JpsModelUtil.findModuleForFile(
                project,
                new File("ModuleXYZ", "Foo.java"));
        assertNull(moduleFound);
    }

    /**
     * Test for {@link JpsModelUtil#findModuleForFile(org.jetbrains.jps.model.JpsProject, java.io.File)} Case: modules
     * side-by-side
     * <pre>
     *     ModuleA
     *     + src
     *       + Foo.java
     *     ModuleB
     *     + src
     *       + Goo.java
     * </pre>
     */
    @Test
    public void testFindModuleForFile_SiblingModules() {
        final JpsProject project = new JpsModelImpl(null).getProject();
        final JpsModule moduleA = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        moduleA.addSourceRoot("file://ModuleA/src", JavaSourceRootType.SOURCE);
        project.addModule(moduleA);
        final JpsModule moduleB = factory.createModule("ModuleB", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        moduleB.addSourceRoot("file://ModuleB/src", JavaSourceRootType.SOURCE);
        project.addModule(moduleB);

        // search in ModuleA
        JpsModule actualModule = JpsModelUtil.findModuleForFile(project, new File(new File("ModuleA", "src"), "Foo.java"));
        assertNotNull(actualModule);
        assertEquals(moduleA, actualModule);

        // search in ModuleB
        actualModule = JpsModelUtil.findModuleForFile(project, new File(new File("ModuleB", "src"), "Goo.java"));
        assertNotNull(actualModule);
        assertEquals(moduleB, actualModule);

        // search outside
        actualModule = JpsModelUtil.findModuleForFile(project, new File("ModuleXYZ", "Hoo.java"));
        assertNull(actualModule);
    }

    /**
     * Test for {@link JpsModelUtil#findModuleForFile(org.jetbrains.jps.model.JpsProject, java.io.File)} Case: modules
     * nested in each other
     * <pre>
     *     ModuleA
     *     + src
     *       + Foo.java
     *       + ModuleB
     *         + src
     *           + Goo.java
     * </pre>
     */
    @Test
    public void testFindModuleForFile_NestedModules() {
        final JpsProject project = new JpsModelImpl(null).getProject();
        final JpsModule moduleA = factory.createModule("ModuleA", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        moduleA.addSourceRoot("file://ModuleA/src", JavaSourceRootType.SOURCE);
        project.addModule(moduleA);
        final JpsModule moduleB = factory.createModule("ModuleB", JpsJavaModuleType.INSTANCE, factory.createDummyElement());
        moduleB.addSourceRoot("file://ModuleA/src/ModuleB/src", JavaSourceRootType.SOURCE);
        project.addModule(moduleB);

        // search in ModuleA
        JpsModule actualModule = JpsModelUtil.findModuleForFile(project, new File(new File("ModuleA", "src"), "Foo.java"));
        assertNotNull(actualModule);
        assertEquals(moduleA, actualModule);

        // search in ModuleB
        actualModule = JpsModelUtil.findModuleForFile(project,
                new File(new File(new File(new File("ModuleA", "src"), "ModuleB"), "src"), "Goo.java"));
        assertNotNull(actualModule);
        assertEquals(moduleB, actualModule);

        // search outside
        actualModule = JpsModelUtil.findModuleForFile(project, new File("ModuleXYZ", "Hoo.java"));
        assertNull(actualModule);
    }

    /**
     * Returns a JpsProject with a Clover configuration attached to it.
     *
     * @param isCloverEnabled          toggle
     * @param isBuildWithCloverEnabled toggle
     * @return JpsProject
     */
    protected JpsProject createProjectWithCloverConfig(boolean isCloverEnabled, boolean isBuildWithCloverEnabled) {
        final CloverPluginConfig config = new MappedCloverPluginConfig();
        config.setEnabled(isCloverEnabled);
        config.setBuildWithClover(isBuildWithCloverEnabled);
        final JpsProject jpsProject = new JpsProjectImpl(new JpsModelImpl(null), null);
        jpsProject.getContainer().setChild(
                CloverJpsProjectConfigurationSerializer.CloverProjectConfigurationRole.INSTANCE,
                new JpsSimpleElementImpl<>(config));
        return jpsProject;
    }

    /**
     * Returns a JpsProject without a Clover configuration.
     *
     * @return JpsProject
     */
    protected JpsProject createProjectWithoutCloverConfig() {
        return new JpsProjectImpl(new JpsModelImpl(null), null);
    }

}
